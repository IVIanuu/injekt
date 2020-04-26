package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.ensureBound
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.cast

class BindingIntrinsicTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private lateinit var moduleFragment: IrModuleFragment

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        moduleFragment = declaration
        return super.visitModuleFragment(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        val callee = expression.symbol.owner

        if (callee.valueParameters.none {
                it.type.toKotlinType().constructor.declarationDescriptor ==
                        symbols.bindingDefinition.descriptor.classDescriptor
            }
        ) return expression

        var bindingOverloadFunction: IrFunction? = null

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (bindingOverloadFunction != null || declaration == callee) return declaration
                val otherFunction = declaration.symbol.owner
                if ((otherFunction.extensionReceiverParameter
                        ?: otherFunction.dispatchReceiverParameter)?.type == callee.extensionReceiverParameter?.type &&
                    otherFunction.typeParameters.size == callee.typeParameters.size &&
                    otherFunction.valueParameters.size == callee.valueParameters.size &&
                    otherFunction.valueParameters.all { otherValueParameter ->
                        val calleeValueParameter = callee.valueParameters[otherValueParameter.index]
                        if (otherValueParameter.type.toKotlinType().constructor.declarationDescriptor == symbols.binding.descriptor) {
                            calleeValueParameter.type.toKotlinType()
                                .constructor.declarationDescriptor == symbols.bindingDefinition.descriptor.classDescriptor
                        } else {
                            otherValueParameter.name == calleeValueParameter.name
                        }
                    }
                ) {
                    bindingOverloadFunction = otherFunction
                    return declaration
                }
                return super.visitFunction(declaration)
            }
        })

        if (bindingOverloadFunction == null) {
            fun MemberScope.findBindingOverloadFunction() = try {
                findFirstFunction(callee.name.asString()) { otherFunction ->
                    val otherReceiver = (otherFunction.extensionReceiverParameter
                        ?: otherFunction.dispatchReceiverParameter)?.type
                    val calleeReceiver = (callee.descriptor.extensionReceiverParameter
                        ?: callee.descriptor.dispatchReceiverParameter)?.type

                    otherFunction != callee.descriptor &&
                            otherReceiver == calleeReceiver &&
                            otherFunction.typeParameters.size == callee.typeParameters.size &&
                            otherFunction.valueParameters.size == callee.valueParameters.size &&
                            otherFunction.valueParameters.all { otherValueParameter ->
                                val calleeValueParameter =
                                    callee.valueParameters[otherValueParameter.index]
                                if (otherValueParameter.type.constructor.declarationDescriptor == symbols.binding.descriptor) {
                                    calleeValueParameter.type.toKotlinType()
                                        .constructor.declarationDescriptor == symbols.bindingDefinition.descriptor.classDescriptor
                                } else {
                                    otherValueParameter.name == calleeValueParameter.name
                                }
                            }
                }
            } catch (e: Exception) {
                null
            }

            bindingOverloadFunction =
                ((callee.descriptor.containingDeclaration as? PackageFragmentDescriptor)
                    ?.let { context.moduleDescriptor.getPackage(it.fqName).memberScope }
                    ?.findBindingOverloadFunction()
                    ?: ((callee.descriptor.dispatchReceiverParameter?.value?.type?.constructor?.declarationDescriptor as? ClassDescriptor)
                        ?.unsubstitutedMemberScope?.findBindingOverloadFunction())
                    ?: (callee.descriptor.extensionReceiverParameter?.value?.type?.constructor?.declarationDescriptor as? ClassDescriptor)
                        ?.unsubstitutedMemberScope?.findBindingOverloadFunction())
                    ?.let { symbolTable.referenceFunction(it) }
                    ?.ensureBound(context.irProviders)
                    ?.owner
        }

        if (bindingOverloadFunction == null) return expression

        return DeclarationIrBuilder(context, expression.symbol).run {
            irCall(bindingOverloadFunction!!).apply {
                if (callee.dispatchReceiverParameter != null) {
                    dispatchReceiver = expression.dispatchReceiver
                    extensionReceiver = expression.extensionReceiver
                } else if (callee.extensionReceiverParameter != null) {
                    if (bindingOverloadFunction!!.dispatchReceiverParameter != null) {
                        dispatchReceiver = expression.extensionReceiver
                    } else {
                        extensionReceiver = expression.extensionReceiver
                    }
                }

                copyTypeArgumentsFrom(expression)

                bindingOverloadFunction!!.valueParameters.forEach { valueParameter ->
                    if (valueParameter.type.toKotlinType().constructor.declarationDescriptor == symbols.binding.descriptor) {
                        putValueArgument(
                            valueParameter.index,
                            bindingExpressionFromDefinition(
                                expression.getValueArgument(valueParameter.index)
                                    .cast()
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
    }

    private fun IrBuilderWithScope.bindingExpressionFromDefinition(
        definition: IrFunctionExpression
    ): IrExpression = irBlock {
        val definitionFunction = definition.function

        val dependencies = mutableListOf<IrCall>()

        definitionFunction.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                val callee = expression.symbol.owner
                if (callee.name.asString() == "get" &&
                    (callee.extensionReceiverParameter
                        ?: callee.dispatchReceiverParameter)?.type
                        ?.classifierOrNull == symbols.bindingDsl
                ) {
                    dependencies += expression
                }
                return expression
            }
        })

        if (dependencies.isNotEmpty()) {
            val linked = linkedBinding(
                dependencies, definitionFunction, Visibilities.PRIVATE,
                Name.identifier("Linked")
            )
            val unlinked =
                unlinkedBinding(dependencies, definitionFunction, linked.constructors.single())
            +unlinked
            unlinked.parent = definitionFunction.parent
            unlinked.addChild(linked)
            linked.parent = unlinked
            +irCall(unlinked.constructors.single())
        } else {
            val linked = linkedBinding(
                dependencies, definitionFunction, Visibilities.LOCAL,
                Name.special("<binding for ${definitionFunction.fqNameWhenAvailable}>")
            )
            +linked
            linked.parent = definitionFunction.parent
            +irGetObject(linked.symbol)
        }
    }

    private fun IrBuilderWithScope.unlinkedBinding(
        dependencies: List<IrCall>,
        definitionFunction: IrFunction,
        linkedConstructor: IrConstructor
    ) = buildClass {
        kind = ClassKind.CLASS
        name = Name.special("<binding for ${definitionFunction.fqNameWhenAvailable}>")
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        superTypes += symbols.unlinkedBinding
            .typeWith(definitionFunction.returnType)

        addConstructor {
            returnType = defaultType
            isPrimary = true
        }.apply {
            body = irBlockBody {
                +IrDelegatingConstructorCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.unitType,
                    symbols.unlinkedBinding
                        .constructors.single()
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
            this.name = Name.identifier("link")
            returnType = symbols.linkedBinding
                .typeWith(definitionFunction.returnType)
        }.apply {
            dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

            overriddenSymbols += symbols.unlinkedBinding
                .functions
                .single { it.owner.name.asString() == "link" }

            val linkerParameter = addValueParameter(
                "linker",
                symbols.linker.defaultType
            )

            body = irExprBody(
                irCall(linkedConstructor).apply {
                    dependencies.forEachIndexed { index, dependency ->
                        val keyOrQualifier = dependency.getValueArgument(0)

                        val linkerGet = symbols.linker.functions
                            .single { function ->
                                if (function.descriptor.name.asString() != "get") {
                                    false
                                } else {
                                    val firstParameter = function.descriptor.valueParameters.first()
                                        .type.constructor.declarationDescriptor
                                    if (keyOrQualifier != null && keyOrQualifier.type.classifierOrFail.descriptor ==
                                        symbols.key.descriptor
                                    ) {
                                        firstParameter == symbols.key.descriptor
                                    } else {
                                        firstParameter == context.builtIns.kClass
                                    }
                                }
                            }
                        putValueArgument(
                            index,
                            irCall(linkerGet).apply {
                                dispatchReceiver = irGet(linkerParameter)
                                putTypeArgument(0, dependency.type)
                                putValueArgument(0, keyOrQualifier)
                            }
                        )
                    }
                }
            )
        }
    }

    private fun IrBuilderWithScope.linkedBinding(
        dependencies: List<IrCall>,
        definitionFunction: IrFunction,
        visibility: Visibility,
        name: Name
    ) = buildClass {
        this.visibility = visibility
        kind = if (dependencies.isNotEmpty()) ClassKind.CLASS else ClassKind.OBJECT
        this.name = name
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        superTypes += symbols.linkedBinding
            .typeWith(definitionFunction.returnType)

        var fieldIndex = 0
        val providerFieldsByDependencies = dependencies.associateWith {
            addField(
                "p${fieldIndex++}",
                symbols.provider.typeWith(it.type)
            )
        }

        addConstructor {
            returnType = defaultType
            isPrimary = true
        }.apply {
            val valueParametersByFields = providerFieldsByDependencies.values.associateWith {
                addValueParameter(
                    it.name.asString(),
                    it.type
                )
            }

            body = irBlockBody {
                +IrDelegatingConstructorCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.unitType,
                    symbols.linkedBinding
                        .constructors.single()
                )
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    this@clazz.symbol,
                    context.irBuiltIns.unitType
                )

                valueParametersByFields.forEach { (field, valueParameter) ->
                    +irSetField(
                        irGet(thisReceiver!!),
                        field,
                        irGet(valueParameter)
                    )
                }
            }
        }

        addFunction {
            this.name = Name.identifier("invoke")
            returnType = definitionFunction.returnType
        }.apply {
            dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

            overriddenSymbols += symbols.provider
                .functions
                .single {
                    it.descriptor.valueParameters.size == 1 &&
                            it.owner.name.asString() == "invoke"
                }

            val parametersParameter = addValueParameter(
                "parameters",
                symbols.parameters.defaultType
            )

            body = definitionFunction.body!!
            definitionFunction.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitReturn(expression: IrReturn): IrExpression {
                    return if (expression.returnTargetSymbol != definitionFunction.symbol) {
                        super.visitReturn(expression)
                    } else {
                        at(expression.startOffset, expression.endOffset)
                        DeclarationIrBuilder(
                            this@BindingIntrinsicTransformer.context,
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
                    return providerFieldsByDependencies[expression]?.let { field ->
                        val invokeFunction = symbols.provider.functions.single {
                            it.owner.name.asString() == "invoke" &&
                                    it.owner.valueParameters.size == if (expression.getValueArgument(
                                    1
                                ) != null
                            )
                                1 else 0
                        }
                        irCall(invokeFunction).apply {
                            dispatchReceiver = irGetField(
                                irGet(dispatchReceiverParameter!!),
                                field
                            )
                            expression.getValueArgument(1)?.let {
                                putValueArgument(0, it)
                            }
                        }
                    } ?: expression
                }

                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return if (expression.symbol == definitionFunction.valueParameters.single().symbol) {
                        irGet(parametersParameter)
                    } else super.visitGetValue(expression)
                }
            })
        }
    }

}
