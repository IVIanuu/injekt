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
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
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
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
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
    private val project: Project,
    pluginContext: IrPluginContext,
    private val bindingTrace: BindingTrace,
    private val moduleStore: ModuleStore
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

        val componentsByCalls = componentCalls.associateWith { componentCall ->
            DeclarationIrBuilder(pluginContext, componentCall.symbol).run {
                val key = componentCall.getValueArgument(0) as IrConstImpl<String>
                val componentDefinition = componentCall.getValueArgument(1) as IrFunctionExpression
                val component = componentClass(componentDefinition)
                val file = fileByCall[componentCall]!!
                file.addChild(component)

                declaration.addClass(
                    pluginContext.psiSourceManager.cast(),
                    project,
                    buildClass {
                        name = Name.identifier(
                            "${key.value}\$${component.fqNameForIrSerialization.asString()
                                .replace(".", "_")}"
                        )
                    }.apply clazz@{
                        createImplicitParameterDeclarationWithWrappedDescriptor()

                        addConstructor {
                            origin = InjektDeclarationOrigin
                            isPrimary = true
                            visibility = Visibilities.PUBLIC
                        }.apply {
                            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                                +IrDelegatingConstructorCallImpl(
                                    startOffset, endOffset,
                                    context.irBuiltIns.unitType,
                                    pluginContext.symbolTable.referenceConstructor(
                                        context.builtIns.any.unsubstitutedPrimaryConstructor!!
                                    )
                                )
                                +IrInstanceInitializerCallImpl(
                                    startOffset,
                                    endOffset,
                                    this@clazz.symbol,
                                    context.irBuiltIns.unitType
                                )
                            }
                        }
                    },
                    FqName("com.ivianuu.injekt.internal.components")
                )
                component
            }
        }

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                return componentsByCalls[expression]?.constructors?.single()
                    ?.let {
                        DeclarationIrBuilder(pluginContext, expression.symbol)
                            .irCall(it.symbol)
                    }
                    ?: expression
            }
        })

        return declaration
    }

    private fun IrBuilderWithScope.componentClass(
        componentDefinition: IrFunctionExpression
    ): IrClass {
        return buildClass {
            kind = ClassKind.CLASS
            origin = InjektDeclarationOrigin
            this.name = Name.identifier("Component${componentDefinition.startOffset}")
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
                moduleStore.getModule(moduleFqName)
            }

            val modules = modulesByCalls.values.toList()

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
            }, moduleStore)

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

                    providerFields.forEach {
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

            annotations += irCallConstructor(
                symbolTable.referenceConstructor(componentMetadata.constructors.single())
                    .ensureBound(pluginContext.irProviders),
                emptyList()
            ).apply {
            }
        }
    }

    private fun IrBuilderWithScope.providerInitializer(
        binding: Binding,
        component: () -> IrExpression,
        providerFields: Map<Key, IrField>
    ): IrExpression = when (binding.bindingType) {
        is Binding.BindingType.ModuleProvider -> {
            val provider = binding.bindingType.provider
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
