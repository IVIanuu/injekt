package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolve.Binding
import com.ivianuu.injekt.compiler.resolve.Graph
import com.ivianuu.injekt.compiler.resolve.Key
import com.ivianuu.injekt.compiler.resolve.ModuleWithAccessor
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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class ComponentTransformer(
    pluginContext: IrPluginContext,
    private val bindingTrace: BindingTrace,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(pluginContext) {

    private val component = getTopLevelClass(InjektClassNames.Component)
    private val componentMetadata = getTopLevelClass(InjektClassNames.ComponentMetadata)
    private val provider = getTopLevelClass(InjektClassNames.Provider)

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        super.visitModuleFragment(declaration)

        val componentCalls = mutableListOf<IrCall>()
        val fileByCall = mutableMapOf<IrCall, IrFile>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
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

        val componentResultsByCalls = componentCalls.associateWith { componentCall ->
            DeclarationIrBuilder(pluginContext, componentCall.symbol).run {
                val componentDefinition = componentCall.getValueArgument(1) as IrFunctionExpression
                val file = fileByCall[componentCall]!!
                val component = componentClass(
                    componentDefinition,
                    getComponentFqName(componentCall, file).shortName()
                )
                file.addChild(component.first)
                component
            }
        }

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                val (component, valueParametersByCaptures) = componentResultsByCalls[expression]
                    ?: return expression
                return DeclarationIrBuilder(pluginContext, expression.symbol).run {
                    val constructor = component.constructors.single()
                    irCall(constructor).apply {
                        valueParametersByCaptures.forEach { (capture, valueParameter) ->
                            putValueArgument(valueParameter.index, capture)
                        }
                    }
                }
            }
        })

        return declaration
    }

    private fun IrBuilderWithScope.componentClass(
        componentDefinition: IrFunctionExpression,
        name: Name
    ): Pair<IrClass, Map<IrGetValue, IrValueParameter>> {
        val valueParametersByCapture = mutableMapOf<IrGetValue, IrValueParameter>()

        return buildClass {
            kind = ClassKind.CLASS
            origin = InjektDeclarationOrigin
            this.name = name
            modality = Modality.FINAL
            visibility = Visibilities.PUBLIC
        }.apply clazz@{
            superTypes += component.defaultType.toIrType()

            createImplicitParameterDeclarationWithWrappedDescriptor()

            val moduleCalls = mutableListOf<IrCall>()

            componentDefinition.function.body!!.transformChildrenVoid(object :
                IrElementTransformerVoid() {
                override fun visitCall(expression: IrCall): IrExpression {
                    super.visitCall(expression)
                    if (expression.symbol.descriptor.annotations.hasAnnotation(InjektClassNames.Module)) {
                        moduleCalls += expression
                    }
                    return expression
                }
            })

            val modulesByCalls = moduleCalls.associateWith {
                val moduleFqName =
                    it.symbol.owner.fqNameForIrSerialization
                        .parent()
                        .child(Name.identifier("${it.symbol.owner.name}\$Impl"))
                declarationStore.getModule(moduleFqName)
            }


            val captures = mutableListOf<IrGetValue>()
            componentDefinition.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    if (expression.symbol != componentDefinition.function.extensionReceiverParameter?.symbol) {
                        captures += expression
                    }
                    return super.visitGetValue(expression)
                }
            })

            val modules = modulesByCalls.values.toList()

            // todo do not include fields for modules which arent't referenced somewhere
            val moduleFields = modules.associateWith {
                addField(
                    it.name,
                    it.defaultType,
                    Visibilities.PRIVATE
                )
            }

            val graph = Graph(pluginContext, bindingTrace, modules.map { module ->
                ModuleWithAccessor(module) {
                    irGetField(
                        irGet(thisReceiver!!),
                        moduleFields.getValue(module)
                    )
                }
            }, declarationStore)

            val providerFields = mutableMapOf<Binding, IrField>()

            graph.componentBindings.forEach { (key, binding) ->
                addField(
                    fieldName = "${binding.module.name}_${key.fieldName}",
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

                    modulesByCalls.forEach { (call, module) ->
                        +irSetField(
                            irGet(thisReceiver!!),
                            moduleFields.getValue(module),
                            irCall(
                                module.constructors.single().symbol,
                                module.defaultType
                            ).apply {
                                copyValueArgumentsFrom(call, call.symbol.owner, symbol.owner)
                            }
                        )
                    }

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
                    pluginContext.irBuiltIns.stringType
                )

                body = irExprBody(
                    DeclarationIrBuilder(pluginContext, symbol).irReturn(
                        irWhen(
                            type = returnType,
                            branches = graph.componentBindings
                                .map { (key, binding) ->
                                    irBranch(
                                        condition = irEquals(
                                            irString(key.keyConstant),
                                            irGet(valueParameters.single())
                                        ),
                                        result = irCall(
                                            symbolTable.referenceSimpleFunction(
                                                provider.unsubstitutedMemberScope
                                                    .findSingleFunction(Name.identifier("invoke"))
                                            )
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
                graph.componentBindings,
                providerFields
            )
        } to valueParametersByCapture
    }

    private fun IrBuilderWithScope.providerInitializer(
        binding: Binding,
        component: () -> IrExpression,
        providerFields: Map<Key, IrField>
    ): IrExpression = when (binding.bindingType) {
        is Binding.BindingType.ModuleProvider -> {
            val provider = binding.bindingType.provider
            if (provider.kind == ClassKind.OBJECT) {
                irGetObject(provider.symbol)
            } else {
                val constructor = provider.constructors.single()
                irCall(callee = constructor).apply {
                    val needsModule =
                        constructor.valueParameters.firstOrNull()?.name?.asString() == "module"
                    if (needsModule) {
                        putValueArgument(0, binding.bindingType.module.accessor())
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
        }
    }

    private fun IrBuilderWithScope.componentMetadata(
        bindings: Map<Key, Binding>,
        providers: Map<Binding, IrField>
    ): IrConstructorCall {
        return irCallConstructor(
            symbolTable.referenceConstructor(componentMetadata.constructors.single())
                .ensureBound(pluginContext.irProviders),
            emptyList()
        ).apply {
            // binding keys
            putValueArgument(
                2,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    bindings.map {
                        irString(
                            it.key.type.constructor
                                .declarationDescriptor!!.fqNameSafe
                                .asString()
                        )
                    }
                )
            )
            // binding providers
            putValueArgument(
                3,
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
