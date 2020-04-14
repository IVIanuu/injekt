package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class BindingDefinitionTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val bindingDefinition = getTypeAlias(InjektClassNames.BindingDefinition)
    private val bindingProvider = getClass(InjektClassNames.BindingProvider)
    private val linker = getClass(InjektClassNames.Linker)
    private val parameters = getClass(InjektClassNames.Parameters)
    private val providerContext = getClass(InjektClassNames.ProviderContext)

    private lateinit var moduleFragment: IrModuleFragment

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        moduleFragment = declaration
        return super.visitModuleFragment(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        val callee = expression.symbol.owner

        if (!callee.annotations.hasAnnotation(InjektClassNames.DslOverloadStub)) return expression

        var dslOverloadFunction: IrFunction? = null

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (dslOverloadFunction != null) return declaration
                val otherFunction = declaration.symbol.owner
                if (otherFunction.annotations.hasAnnotation(InjektClassNames.KeyOverload) &&
                    (otherFunction.extensionReceiverParameter
                        ?: otherFunction.dispatchReceiverParameter)?.type == callee.extensionReceiverParameter?.type &&
                    otherFunction.typeParameters.size == callee.typeParameters.size &&
                    otherFunction.valueParameters.size == callee.valueParameters.size &&
                    otherFunction.valueParameters.all { otherValueParameter ->
                        val calleeValueParameter = callee.valueParameters[otherValueParameter.index]
                        if (otherValueParameter.type.toKotlinType().constructor.declarationDescriptor == bindingProvider) {
                            calleeValueParameter.type.toKotlinType().constructor.declarationDescriptor == bindingDefinition.classDescriptor
                        } else {
                            otherValueParameter.name == calleeValueParameter.name
                        }
                    }
                ) {
                    dslOverloadFunction = otherFunction
                    return declaration
                }
                return super.visitFunction(declaration)
            }
        })

        if (dslOverloadFunction == null) {
            fun MemberScope.findDslOverloadFunction() = try {
                findFirstFunction(callee.name.asString()) { otherFunction ->
                    otherFunction.annotations.hasAnnotation(InjektClassNames.DslOverload)
                            && (otherFunction.extensionReceiverParameter
                        ?: otherFunction.dispatchReceiverParameter)?.type == callee.extensionReceiverParameter?.type?.toKotlinType() &&
                            otherFunction.typeParameters.size == callee.typeParameters.size &&
                            otherFunction.valueParameters.size == callee.valueParameters.size &&
                            otherFunction.valueParameters.all { otherValueParameter ->
                                val calleeValueParameter =
                                    callee.valueParameters[otherValueParameter.index]
                                if (otherValueParameter.type.constructor.declarationDescriptor == bindingProvider) {
                                    calleeValueParameter.type.toKotlinType().constructor.declarationDescriptor == bindingDefinition.classDescriptor
                                } else {
                                    otherValueParameter.name == calleeValueParameter.name
                                }
                            }
                }
            } catch (e: Exception) {
                null
            }

            dslOverloadFunction =
                ((callee.descriptor.containingDeclaration as PackageFragmentDescriptor)
                    .let { pluginContext.moduleDescriptor.getPackage(it.fqName).memberScope }
                    .findDslOverloadFunction()
                    ?: (callee.descriptor.extensionReceiverParameter?.value?.type?.constructor?.declarationDescriptor as? ClassDescriptor)
                        ?.unsubstitutedMemberScope?.findDslOverloadFunction())
                    ?.let { symbolTable.referenceFunction(it) }
                    ?.ensureBound()
                    ?.owner
        }

        checkNotNull(dslOverloadFunction) {
            "Couldn't find @DslOverload function for ${callee.dump()}"
        }

        val builder = DeclarationIrBuilder(pluginContext, expression.symbol)

        return builder.irCall(dslOverloadFunction!!).apply {
            if (dslOverloadFunction!!.dispatchReceiverParameter != null) {
                dispatchReceiver = expression.extensionReceiver
            } else {
                extensionReceiver = expression.extensionReceiver
            }

            copyTypeArgumentsFrom(expression)

            dslOverloadFunction!!.valueParameters.forEach { valueParameter ->
                if (valueParameter.type.toKotlinType().constructor.declarationDescriptor == bindingProvider) {
                    putValueArgument(
                        valueParameter.index,
                        builder.bindingProvider(
                            dslOverloadFunction!!.typeParameters
                                .associate { it.symbol to expression.getTypeArgument(it.index)!! },
                            expression.getValueArgument(valueParameter.index)!!
                        )
                    )
                } else {
                    putValueArgument(
                        valueParameter.index,
                        expression.getValueArgument(valueParameter.index)
                    )
                }
            }
        }
    }

    private fun IrBuilderWithScope.bindingProvider(
        typeParameters: Map<IrTypeParameterSymbol, IrType>,
        definitionExpression: IrExpression
    ): IrExpression {
        val bindingProviderType = bindingProvider.defaultType.toIrType()
            .substitute(typeParameters)

        return irBlock(
            origin = IrStatementOrigin.OBJECT_LITERAL,
            resultType = bindingProviderType
        ) {
            val irClass = buildClass {
                visibility = Visibilities.LOCAL
                name = Name.special("<binding provider>")
                origin = InjektOrigin
            }.apply clazz@{
                superTypes = listOf(bindingProviderType)
                createImplicitParameterDeclarationWithWrappedDescriptor()

                val dependencies = mutableListOf<IrCall>()

                definitionExpression.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitCall(expression: IrCall): IrExpression {
                        val callee = expression.symbol.owner
                        if (callee.name.asString() == "get" &&
                            (callee.extensionReceiverParameter
                                ?: callee.dispatchReceiverParameter)?.descriptor?.type?.constructor == providerContext
                        ) {
                            dependencies += expression
                        }
                        return super.visitCall(expression)
                    }
                })

                var i = 0
                val fieldsByExpression = dependencies
                    .associateWith { expression ->
                        addField {
                            name = Name.identifier("provider$i")
                            type = symbolTable.referenceClass(bindingProvider)
                                .ensureBound()
                                .typeWith(expression.type)
                        }.also { i++ }
                    }

                definitionExpression.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitCall(expression: IrCall): IrExpression {
                        super.visitCall(expression)
                        return fieldsByExpression[expression]?.let {
                            irCall(
                                symbolTable.referenceSimpleFunction(
                                    bindingProvider.findFirstFunction("invoke") {
                                        it.valueParameters.size == 1
                                    }
                                ),
                                expression.type
                            ).apply {
                                dispatchReceiver = irGetField(irGet(thisReceiver!!), it)
                                if (expression.symbol.owner.valueParameters) {

                                } else {

                                }
                            }
                        } ?: expression
                    }
                })

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
                    }
                }

                addFunction {
                    name = Name.identifier("link")
                    returnType = pluginContext.irBuiltIns.unitType
                }.apply {
                    dispatchReceiverParameter = thisReceiver!!.copyTo(this)
                    addValueParameter(
                        "linker",
                        linker.defaultType.toIrType()
                    )
                    body = irBlockBody {
                        fieldsByExpression.forEach { (expression, providerField) ->
                            //val linkerGet = if (expression.symbol.owner.descriptor.name.)
                            irSetField(
                                irGet(thisReceiver!!),
                                providerField,
                                irCall(
                                    symbolTable.referenceSimpleFunction(
                                        injektPackage.memberScope.findFirstFunction("get") {
                                            it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor ==
                                                    linker && it.valueParameters.size == 1
                                        }
                                    ),
                                    providerField.type
                                ).apply {

                                }
                            )
                        }
                    }
                }

                addFunction {
                    name = Name.identifier("invoke")
                    returnType = typeParameters.values.single()
                }.apply {
                    dispatchReceiverParameter = thisReceiver!!.copyTo(this)
                    addValueParameter(
                        "parameters",
                        parameters.defaultType.toIrType()
                    )
                    body = irBlockBody {
                        +irReturn(irNull())
                    }
                }
            }

            +irClass
            +IrConstructorCallImpl.fromSymbolDescriptor(
                startOffset, endOffset, irClass.defaultType,
                irClass.constructors.single().symbol,
                IrStatementOrigin.OBJECT_LITERAL
            )
        }
    }
}
