package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektDeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.asTypeName
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getConstant
import com.ivianuu.injekt.compiler.irTrace
import com.ivianuu.injekt.compiler.resolve.Binding
import com.ivianuu.injekt.compiler.resolve.Graph
import com.ivianuu.injekt.compiler.resolve.Key
import com.ivianuu.injekt.compiler.resolve.ModuleBinding
import com.ivianuu.injekt.compiler.resolve.ModuleWithAccessor
import com.ivianuu.injekt.compiler.resolve.ParentComponentBinding
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.cast

class ComponentTransformer(
    pluginContext: IrPluginContext,
    private val bindingTrace: BindingTrace,
    private val declarationStore: InjektDeclarationStore,
    private val moduleFragment: IrModuleFragment
) : AbstractInjektTransformer(pluginContext) {

    private val component = getTopLevelClass(InjektFqNames.Component)
    private val componentMetadata = getTopLevelClass(InjektFqNames.ComponentMetadata)
    private val provider = getTopLevelClass(InjektFqNames.Provider)
    private val singleProvider = getTopLevelClass(InjektFqNames.SingleProvider)

    private val componentCalls = mutableListOf<IrCall>()
    private val fileByCall = mutableMapOf<IrCall, IrFile>()
    private val valueParametersByCapturesByComponent =
        mutableMapOf<IrClass, Map<IrGetValue, IrValueParameter>>()

    private var computedComponentCalls = false
    private val processedComponentsByKey = mutableMapOf<String, IrClass>()
    private val processedComponentsByCall = mutableMapOf<IrCall, IrClass>()
    private val processingComponents = mutableSetOf<String>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        super.visitModuleFragment(declaration)
        computeComponentCallsIfNeeded()

        componentCalls.forEach { getProcessedComponent(it) }

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)

                return processedComponentsByCall[expression]
                    ?.let {
                        DeclarationIrBuilder(pluginContext, expression.symbol).run {
                            val constructor = it.constructors.single()
                            irCall(constructor).apply {
                                valueParametersByCapturesByComponent[it]?.forEach { (capture, valueParameter) ->
                                    putValueArgument(valueParameter.index, capture)
                                }
                            }
                        }
                    } ?: expression
            }
        })

        return moduleFragment
    }

    fun getProcessedComponent(key: String): IrClass? {
        computeComponentCallsIfNeeded()

        processedComponentsByKey[key]?.let { return it }

        val call = componentCalls.firstOrNull {
            it.getValueArgument(0)!!.getConstant<String>() == key
        } ?: return null

        return getProcessedComponent(call)
    }

    fun getProcessedComponent(call: IrCall): IrClass? {
        computeComponentCallsIfNeeded()

        return DeclarationIrBuilder(pluginContext, call.symbol).run {
            val key = call.getValueArgument(0)!!.getConstant<String>()

            check(key !in processingComponents) {
                "Circular dependency for component $key"
            }
            processingComponents += key

            val componentDefinition = call.getValueArgument(1) as IrFunctionExpression
            val file = fileByCall[call]!!
            val component = componentClass(
                componentDefinition,
                pluginContext.irTrace.get(InjektWritableSlices.COMPONENT_FQ_NAME, call)!!
                    .shortName()
            )
            file.addChild(component)
            processedComponentsByCall[call] = component
            processedComponentsByKey[key] = component
            processingComponents -= key
            component
        }
    }

    private fun computeComponentCallsIfNeeded() {
        if (computedComponentCalls) return
        computedComponentCalls = true

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val fileStack = mutableListOf<IrFile>()
            override fun visitFile(declaration: IrFile): IrFile {
                fileStack.push(declaration)
                return super.visitFile(declaration)
                    .also { fileStack.pop() }
            }
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                if (expression.symbol.descriptor.fqNameSafe
                        .asString() == "com.ivianuu.injekt.Component"
                ) {
                    componentCalls += expression
                    fileByCall[expression] = fileStack.last()
                }
                return expression
            }
        })
    }

    private fun IrBuilderWithScope.componentClass(
        componentDefinition: IrFunctionExpression,
        name: Name
    ): IrClass {
        return buildClass {
            kind = ClassKind.CLASS
            origin =
                InjektDeclarationOrigin
            this.name = name
            modality = Modality.FINAL
            visibility = Visibilities.PUBLIC
        }.apply clazz@{
            superTypes += component.defaultType.toIrType()

            createImplicitParameterDeclarationWithWrappedDescriptor()

            val valueParametersByCapture = mutableMapOf<IrGetValue, IrValueParameter>()
            valueParametersByCapturesByComponent[this] = valueParametersByCapture

            val moduleCall = componentDefinition.function.body!!
                .cast<IrExpressionBody>().expression as IrCall

            val moduleFqName = moduleCall.symbol.owner.fqNameForIrSerialization
                .parent()
                .child(Name.identifier("${moduleCall.symbol.owner.name}\$Impl"))

            val module = declarationStore.getModule(moduleFqName)

            val captures = mutableListOf<IrGetValue>()
            componentDefinition.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    if (expression.symbol != componentDefinition.function.extensionReceiverParameter?.symbol) {
                        captures += expression
                    }
                    return super.visitGetValue(expression)
                }
            })

            val moduleField = addField(
                "module",
                module.defaultType,
                Visibilities.PRIVATE
            )

            val graph = Graph(
                context = pluginContext,
                bindingTrace = bindingTrace,
                component = this,
                module = ModuleWithAccessor(module, module.typeParameters.map {
                    it.symbol to moduleCall.getTypeArgument(it.index)!!
                }.toMap()) {
                    irGetField(
                        irGet(thisReceiver!!),
                        moduleField
                    )
                },
                declarationStore = declarationStore
            )

            val providerFields = mutableMapOf<Binding, IrField>()

            graph.allBindings.forEach { (key, binding) ->
                addField(
                    fieldName = "${binding.containingDeclaration.name}_${key.fieldName}",
                    fieldType = provider.defaultType.replace(
                        newArguments = listOf(binding.key.type.asTypeProjection())
                    ).toIrType(),
                    fieldVisibility = Visibilities.PRIVATE
                ).apply {
                    providerFields[binding] = this
                }
            }

            addConstructor {
                returnType = defaultType
                visibility = Visibilities.PUBLIC
                isPrimary = true
            }.apply {
                captures.forEachIndexed { index, capture ->
                    valueParametersByCapture[capture] = addValueParameter(
                        "p$index",
                        capture.type
                    )
                }

                body = irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        context.irBuiltIns.unitType,
                        symbolTable.referenceConstructor(
                            context.builtIns.any
                                .unsubstitutedPrimaryConstructor!!
                        )
                    )
                    +IrInstanceInitializerCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        this@clazz.symbol,
                        context.irBuiltIns.unitType
                    )

                    +irSetField(
                        irGet(thisReceiver!!),
                        moduleField,
                        irCall(
                            module.constructors.single().symbol,
                            module.defaultType
                        ).apply {
                            copyValueArgumentsFrom(
                                moduleCall,
                                moduleCall.symbol.owner,
                                symbol.owner
                            )
                        }
                    )

                    val initializedKeys = mutableSetOf<Key>()
                    val processedFields = mutableSetOf<IrField>()

                    while (true) {
                        val fieldsToProcess = providerFields
                            .filter { it.value !in processedFields }
                        if (fieldsToProcess.isEmpty()) break

                        fieldsToProcess
                            .filter { it.key.dependencies.all { it in initializedKeys } }
                            .forEach {
                                initializedKeys += it.key.key
                                processedFields += it.value
                                +irSetField(
                                    irGet(thisReceiver!!),
                                    it.value,
                                    providerInitializer(
                                        it.key,
                                        { irGet(thisReceiver!!) },
                                        providerFields.mapKeys { it.key.key }
                                    )
                                )
                            }
                    }
                }
            }.apply {
                transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        return valueParametersByCapture[expression]
                            ?.let { irGet(it) }
                            ?: super.visitGetValue(expression)
                    }
                })
            }

            addFunction {
                this.name = Name.identifier("get")
                visibility = Visibilities.PUBLIC
                returnType = pluginContext.irBuiltIns.anyNType
            }.apply getFunction@{
                dispatchReceiverParameter = thisReceiver?.copyTo(this)

                overriddenSymbols += symbolTable.referenceSimpleFunction(
                    component.unsubstitutedMemberScope.findSingleFunction(Name.identifier("get"))
                )

                addValueParameter(
                    "key",
                    pluginContext.irBuiltIns.intType
                )

                body = irExprBody(
                    DeclarationIrBuilder(pluginContext, symbol).irReturn(
                        irWhen(
                            type = returnType,
                            branches = graph.allBindings
                                .map { (key, binding) ->
                                    irBranch(
                                        condition = irEquals(
                                            irInt(key.keyConstant),
                                            irGet(valueParameters.single())
                                        ),
                                        result = irCall(
                                            symbolTable.referenceSimpleFunction(
                                                provider.unsubstitutedMemberScope
                                                    .findSingleFunction(Name.identifier("invoke"))
                                            ).ensureBound(pluginContext.irProviders)
                                        ).apply {
                                            dispatchReceiver = irGetField(
                                                irGet(dispatchReceiverParameter!!),
                                                providerFields.getValue(binding)
                                            )
                                        }
                                    )
                                } + irElseBranch(
                                irCall(
                                    symbolTable.referenceFunction(
                                        component.unsubstitutedMemberScope
                                            .findSingleFunction(Name.identifier("get"))
                                    ).ensureBound(pluginContext.irProviders).owner,
                                    InjektStatementOrigin,
                                    symbolTable.referenceClass(component)
                                ).apply {
                                    val original = this@getFunction
                                    dispatchReceiver = irGet(original.dispatchReceiverParameter!!)
                                    putValueArgument(0, irGet(original.valueParameters.single()))
                                }
                            )
                        )
                    )
                )
            }

            annotations += componentMetadata(
                graph.allScopes,
                graph.allBindings,
                providerFields
            )

            check(descriptor.annotations.hasAnnotation(InjektFqNames.ComponentMetadata))
        }
    }

    private fun IrBuilderWithScope.providerInitializer(
        binding: Binding,
        component: () -> IrExpression,
        providerFields: Map<Key, IrField>
    ): IrExpression = when (binding) {
        is ParentComponentBinding -> {
            val provider = binding.providerField
            irGetField(binding.componentWithAccessor.accessor(), provider)
        }
        is ModuleBinding -> {
            val provider = binding.provider
            val providerExpression = if (provider.kind == ClassKind.OBJECT) {
                irGetObject(provider.symbol)
            } else {
                val constructor = provider.constructors.single()
                irCall(callee = constructor).apply {
                    val needsModule =
                        constructor.valueParameters.firstOrNull()?.name?.asString() == "module"
                    if (needsModule) {
                        putValueArgument(0, binding.module.accessor())
                    }
                    binding.dependencies.forEachIndexed { index, key ->
                        putValueArgument(
                            if (needsModule) index + 1 else index,
                            irGetField(
                                component(),
                                providerFields[key]!!
                            )
                        )
                    }
                }
            }

            if (binding.isSingle) {
                irCall(
                    callee = symbolTable.referenceConstructor(
                        singleProvider.unsubstitutedPrimaryConstructor!!
                    ).ensureBound(pluginContext.irProviders).owner,
                ).apply {
                    putValueArgument(0, providerExpression)
                }
            } else {
                providerExpression
            }
        }
    }

    private fun IrBuilderWithScope.componentMetadata(
        scopes: Set<FqName>,
        bindings: Map<Key, Binding>,
        providers: Map<Binding, IrField>
    ): IrConstructorCall {
        return irCallConstructor(
            symbolTable.referenceConstructor(componentMetadata.constructors.single())
                .ensureBound(pluginContext.irProviders),
            emptyList()
        ).apply {
            // scopes
            putValueArgument(
                0,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    scopes.map { irString(it.asString()) }
                )
            )

            // binding keys
            putValueArgument(
                1,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    bindings.map {
                        irString(it.key.type.asTypeName()!!.toString())
                    }
                )
            )
            // binding providers
            putValueArgument(
                2,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    providers.map {
                        irString(it.value.name.asString())
                    }
                )
            )
        }
    }
}
