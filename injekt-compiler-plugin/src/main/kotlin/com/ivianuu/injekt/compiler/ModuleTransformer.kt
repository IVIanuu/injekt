package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.cast

class ModuleTransformer(pluginContext: IrPluginContext) : AbstractInjektTransformer(pluginContext) {

    private val moduleMetadata = getTopLevelClass(InjektClassNames.ModuleMetadata)
    private val provider = getTopLevelClass(InjektClassNames.Provider)
    private val providerDsl = getTopLevelClass(InjektClassNames.ProviderDsl)

    override fun visitFile(declaration: IrFile): IrFile {
        super.visitFile(declaration)

        val moduleFunctions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                val parent = declaration.parent as? IrClass
                if (declaration.annotations.hasAnnotation(InjektClassNames.Module) ||
                    (parent != null && parent.annotations.hasAnnotation(InjektClassNames.Module))
                ) {
                    moduleFunctions += declaration
                }

                return super.visitFunction(declaration)
            }
        })

        moduleFunctions.forEach { moduleFunction ->
            DeclarationIrBuilder(pluginContext, moduleFunction.symbol).run {
                declaration.addChild(moduleClass(moduleFunction))
                moduleFunction.body = irExprBody(irInjektStubUnit())
            }
        }

        if (declaration is IrFileImpl) {
            declaration.metadata = MetadataSource.File(
                declaration
                    .declarations
                    .map { it.descriptor }
            )
        }

        return declaration
    }

    private fun IrBuilderWithScope.moduleClass(
        function: IrFunction
    ): IrClass {
        return buildClass {
            kind = ClassKind.CLASS
            origin = InjektOrigin
            name = Name.identifier(function.name.asString() + "\$Impl")
            modality = Modality.FINAL
            visibility = function.visibility
        }.apply clazz@{
            createImplicitParameterDeclarationWithWrappedDescriptor()

            var parameterMap = emptyMap<IrValueParameter, IrValueParameter>()
            var fieldsByParameters = emptyMap<IrValueParameter, IrField>()

            addConstructor {
                returnType = defaultType
                visibility = Visibilities.PUBLIC
                isPrimary = true
            }.apply {
                parameterMap = function.valueParameters
                    .associateWith { it.copyTo(this) }
                valueParameters = parameterMap.values.toList()
                fieldsByParameters = valueParameters.associateWith {
                    addField {
                        this.name = it.name
                        type = it.type
                    }
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

                    fieldsByParameters.forEach { (parameter, field) ->
                        irSetField(
                            irGet(thisReceiver!!),
                            field,
                            irGet(parameter)
                        )
                    }
                }
            }

            val definitionCalls = mutableListOf<IrCall>()
            function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitCall(expression: IrCall): IrExpression {
                    super.visitCall(expression)

                    if (expression.symbol.descriptor.name.asString() == "factory") {
                        definitionCalls += expression
                    }
                    return expression
                }
            })

            val providerByDefinitionCall = mutableMapOf<IrCall, IrClass>()

            definitionCalls.forEachIndexed { index, definitionCall ->
                addChild(
                    provider(
                        name = Name.identifier("provider_$index"),
                        definition = definitionCall.getValueArgument(1)!!.cast(),
                        module = this,
                        moduleParametersMap = parameterMap,
                        moduleFieldsByParameter = fieldsByParameters
                    ).also { providerByDefinitionCall[definitionCall] = it }
                )
            }

            annotations += irCallConstructor(
                symbolTable.referenceConstructor(moduleMetadata.constructors.single())
                    .ensureBound(),
                emptyList()
            ).apply {
                if (definitionCalls.isNotEmpty()) {
                    val bindings = definitionCalls
                        .map { call ->
                            irString(
                                call.getTypeArgument(0)!!.toKotlinType().constructor
                                    .declarationDescriptor!!.fqNameSafe
                                    .asString()
                            ) to irString(
                                providerByDefinitionCall[call]!!.name.asString()
                            )
                        }
                    putValueArgument(
                        2,
                        IrVarargImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            pluginContext.irBuiltIns.arrayClass
                                .typeWith(pluginContext.irBuiltIns.stringType),
                            pluginContext.irBuiltIns.stringType,
                            bindings.map { it.first }
                        )
                    )
                    putValueArgument(
                        3,
                        IrVarargImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            pluginContext.irBuiltIns.arrayClass
                                .typeWith(pluginContext.irBuiltIns.stringType),
                            pluginContext.irBuiltIns.stringType,
                            bindings.map { it.second }
                        )
                    )
                }
            }
        }
    }

    private fun IrBuilderWithScope.provider(
        name: Name,
        definition: IrFunctionExpression,
        module: IrClass,
        moduleParametersMap: Map<IrValueParameter, IrValueParameter>,
        moduleFieldsByParameter: Map<IrValueParameter, IrField>
    ): IrClass {
        val definitionFunction = definition.function

        val dependencies = mutableListOf<IrCall>()

        val capturedModuleValueParameters = mutableListOf<IrValueParameter>()

        definitionFunction.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                val callee = expression.symbol.owner
                if (callee.name.asString() == "get" &&
                    (callee.extensionReceiverParameter
                        ?: callee.dispatchReceiverParameter)?.descriptor?.type
                        ?.constructor?.declarationDescriptor == providerDsl
                ) {
                    dependencies += expression
                }
                return expression
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                super.visitGetValue(expression)

                if (moduleParametersMap.keys.any { it.symbol == expression.symbol }) {
                    capturedModuleValueParameters += expression.symbol.owner as IrValueParameter
                }

                return expression
            }
        })

        return buildClass {
            kind = if (dependencies.isNotEmpty() ||
                capturedModuleValueParameters.isNotEmpty()
            ) ClassKind.CLASS else ClassKind.OBJECT
            origin = InjektOrigin
            this.name = name
            modality = Modality.FINAL
            visibility = Visibilities.PUBLIC
        }.apply clazz@{
            val resultType = definition.function.returnType
            superTypes += provider
                .defaultType
                .replace(
                    newArguments = listOf(
                        resultType.toKotlinType().asTypeProjection()
                    )
                )
                .toIrType()

            createImplicitParameterDeclarationWithWrappedDescriptor()

            val moduleField = if (capturedModuleValueParameters.isNotEmpty()) {
                addField {
                    this.name = module.name
                    this.type = module.defaultType
                }
            } else null

            var depIndex = 0
            val fieldsByDependency = dependencies
                .associateWith { expression ->
                    addField {
                        this.name = Name.identifier("p$depIndex")
                        type = symbolTable.referenceClass(provider)
                            .ensureBound()
                            .typeWith(expression.type)
                        visibility = Visibilities.PRIVATE
                    }.also { depIndex++ }
                }

            addTypeParameter("T", resultType)

            addConstructor {
                returnType = defaultType
                visibility = Visibilities.PUBLIC
                isPrimary = true
            }.apply {
                if (moduleField != null) {
                    addValueParameter {
                        this.name = Name.identifier("module")
                        this.type = module.defaultType
                    }
                }

                fieldsByDependency.forEach { (_, field) ->
                    addValueParameter {
                        this.name = field.name
                        this.type = field.type
                    }
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

                    if (moduleField != null) {
                        +irSetField(
                            irGet(thisReceiver!!),
                            moduleField,
                            irGet(valueParameters.first())
                        )
                    }

                    valueParameters
                        .drop(if (moduleField != null) 1 else 0)
                        .forEach { valueParameter ->
                            +irSetField(
                                irGet(thisReceiver!!),
                                fieldsByDependency.values.toList()
                                        [valueParameter.index - if (moduleField != null) 1 else 0],
                                irGet(valueParameter)
                            )
                        }
                }
            }

            addFunction {
                this.name = Name.identifier("invoke")
                returnType = resultType
                visibility = Visibilities.PUBLIC
            }.apply {
                dispatchReceiverParameter = thisReceiver?.copyTo(this)

                overriddenSymbols += symbolTable.referenceSimpleFunction(
                    provider.unsubstitutedMemberScope.findSingleFunction(Name.identifier("invoke"))
                )

                body = definitionFunction.body
                body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        return if (moduleParametersMap.keys.none { it.symbol == expression.symbol }) {
                            super.visitGetValue(expression)
                        } else {
                            val newParameter = moduleParametersMap[expression.symbol.owner]!!
                            val field = moduleFieldsByParameter[newParameter]!!
                            return irGetField(
                                irGetField(
                                    irGet(dispatchReceiverParameter!!),
                                    moduleField!!
                                ),
                                field
                            )
                        }
                    }

                    override fun visitReturn(expression: IrReturn): IrExpression {
                        return if (expression.returnTargetSymbol != definitionFunction.symbol) {
                            super.visitReturn(expression)
                        } else {
                            at(expression.startOffset, expression.endOffset)
                            DeclarationIrBuilder(
                                pluginContext,
                                symbol
                            ).irReturn(expression.value.transform(this, null)).apply {
                                this.returnTargetSymbol
                            }
                        }
                    }

                    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                        if (declaration.parent == definitionFunction)
                            declaration.parent = this@apply
                        return super.visitDeclaration(declaration)
                    }

                    override fun visitCall(expression: IrCall): IrExpression {
                        super.visitCall(expression)
                        return fieldsByDependency[expression]?.let { field ->
                            irCall(
                                symbolTable.referenceSimpleFunction(
                                    provider.findFirstFunction("invoke") { true }
                                ),
                                expression.type
                            ).apply {
                                dispatchReceiver = irGetField(
                                    irGet(dispatchReceiverParameter!!),
                                    field
                                )
                            }
                        } ?: expression
                    }
                })
            }
        }
    }

}