package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irEqualsNull
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irSetVar
import org.jetbrains.kotlin.ir.builders.irTemporaryVar
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class BindingDefinitionTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val bindingDefinition = getClass(InjektClassNames.BindingDefinition)
    private val bindingProvider = getClass(InjektClassNames.BindingProvider)

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
                        if (otherValueParameter.type.toKotlinType().constructor.declarationDescriptor == bindingDefinition) {
                            calleeValueParameter.type.toKotlinType()
                                .isSubtypeOf(bindingProvider.defaultType)
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
                    otherFunction.annotations.hasAnnotation(InjektClassNames.KeyOverload)
                            && (otherFunction.extensionReceiverParameter
                        ?: otherFunction.dispatchReceiverParameter)?.type == callee.extensionReceiverParameter?.type?.toKotlinType() &&
                            otherFunction.typeParameters.size == callee.typeParameters.size &&
                            otherFunction.valueParameters.size == callee.valueParameters.size &&
                            otherFunction.valueParameters.all { otherValueParameter ->
                                val calleeValueParameter =
                                    callee.valueParameters[otherValueParameter.index]
                                if (otherValueParameter.type.constructor.declarationDescriptor == bindingDefinition) {
                                    calleeValueParameter.type.toKotlinType()
                                        .isSubtypeOf(bindingProvider.defaultType)
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
                if (valueParameter.type.toKotlinType().constructor.declarationDescriptor == bindingDefinition) {
                    putValueArgument(
                        valueParameter.index,
                        bindingProvider(expression.getValueArgument(valueParameter.index)!!)
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

    private fun bindingProvider(definitionExpression: IrExpression): IrExpression {
        return DeclarationIrBuilder(pluginContext, function.symbol).irBlock(
            origin = IrStatementOrigin.OBJECT_LITERAL,
            resultType = bindingProvider.defaultType.toIrType()
        ) {
            val irClass = buildClass {
                visibility = Visibilities.LOCAL
                name = Name.special("<function reference to ${function.fqNameWhenAvailable}>")
                origin = InjektOrigin
            }.apply clazz@{
                superTypes = listOf(functionExpression.type)
                createImplicitParameterDeclarationWithWrappedDescriptor()

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
                    name = Name.identifier("invoke")
                    modality = Modality.OPEN
                    returnType = function.returnType
                    isSuspend = false
                }.apply {
                    overriddenSymbols = overriddenSymbols + listOf(
                        functionExpression.type
                            .classOrNull!!
                            .getSimpleFunction("invoke")!!
                    )
                    dispatchReceiverParameter = parentAsClass.thisReceiver!!.copyTo(this)
                    annotations += function.annotations
                    val valueParameterMap =
                        function.explicitParameters.withIndex().associate { (index, param) ->
                            param to param.copyTo(this, type = param.type, index = index)
                        }
                    valueParameters = valueParameters + valueParameterMap.values
                    body = DeclarationIrBuilder(context, symbol).irBlockBody {
                        function.body?.statements?.forEach { statement ->
                            +statement.transform(object : IrElementTransformerVoid() {
                                override fun visitGetValue(expression: IrGetValue): IrExpression {
                                    val replacement = valueParameterMap[expression.symbol.owner]
                                        ?: return super.visitGetValue(expression)

                                    at(expression.startOffset, expression.endOffset)
                                    return irGet(replacement)
                                }

                                override fun visitReturn(expression: IrReturn): IrExpression =
                                    if (expression.returnTargetSymbol != function.symbol) {
                                        super.visitReturn(expression)
                                    } else {
                                        at(expression.startOffset, expression.endOffset)
                                        irReturn(expression.value.transform(this, null))
                                    }

                                override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                                    if (declaration.parent == function)
                                        declaration.parent = this@apply
                                    return super.visitDeclaration(declaration)
                                }
                            }, null)
                        }
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

    private fun transformClass(clazz: IrClass) {
        val invokeFunction = clazz.functions
            .single {
                it.name.asString() == "invoke" &&
                        it.valueParameters.size == 2 &&
                        it.valueParameters[0].type.toKotlinType() == component.defaultType &&
                        it.valueParameters[1].type.toKotlinType() == parameters.defaultType
            }

        if (invokeFunction.body == null) return

        val dependencies = mutableListOf<IrExpression>()

        invokeFunction.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = expression.symbol.ensureBound().owner
                if (callee.name.asString() == "get" &&
                    callee.dispatchReceiverParameter?.type == component.defaultType.toIrType() &&
                    callee.valueParameters.first().type.isSubtypeOfClass(
                        symbolTable.referenceClass(
                            bindingDefinition
                        )
                    ) &&
                    expression.getValueArgument(0)!!.type.isFullyResolved() &&
                    expression.getValueArgument(0)!! is IrGetField
                ) {
                    dependencies += expression
                }
                return super.visitCall(expression)
            }
        })

        if (dependencies.isEmpty()) return

        val fieldsByDependency = mutableMapOf<IrExpression, IrField>()

        dependencies.forEachIndexed { index, dependencyExpression ->
            val providerFieldType = pluginContext.builtIns.getFunction(2)
                .defaultType
                .toIrType()
                .classifierOrFail
                .typeWith(
                    component.defaultType.toIrType(),
                    parameters.defaultType.toIrType(),
                    dependencyExpression.type
                ).makeNullable()

            clazz.addField {
                name = Name.identifier("provider$index")
                type = providerFieldType
                visibility = Visibilities.PRIVATE
            }.also { fieldsByDependency[dependencyExpression] = it }
        }

        val boundComponentField = clazz.addField {
            name = Name.identifier("boundComponent")
            type = component.defaultType.toIrType().makeNullable()
            visibility = Visibilities.PRIVATE
        }

        with(DeclarationIrBuilder(pluginContext, clazz.symbol)) {
            val resetFieldsExpression = irBlock {
                +IrIfThenElseImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.unitType
                ).apply {
                    branches += IrBranchImpl(
                        condition = irEqualsNull(
                            irGetField(
                                irGet(invokeFunction.dispatchReceiverParameter!!),
                                boundComponentField
                            )
                        ),
                        result = irSetField(
                            irGet(invokeFunction.dispatchReceiverParameter!!),
                            boundComponentField,
                            irGet(invokeFunction.valueParameters.first())
                        )
                    )
                    branches += IrBranchImpl(
                        condition = irNot(
                            irEqeqeq(
                                irGetField(
                                    irGet(invokeFunction.dispatchReceiverParameter!!),
                                    boundComponentField
                                ),
                                irGet(invokeFunction.valueParameters.first())
                            )
                        ),
                        result = irBlock {
                            +irSetField(
                                irGet(invokeFunction.dispatchReceiverParameter!!),
                                boundComponentField,
                                irGet(invokeFunction.valueParameters.first())
                            )
                            fieldsByDependency.values.forEach {
                                +irSetField(
                                    irGet(invokeFunction.dispatchReceiverParameter!!),
                                    it,
                                    irNull()
                                )
                            }
                        }
                    )
                }
            }

            invokeFunction.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitCall(expression: IrCall): IrExpression {
                    val fieldForDependency =
                        fieldsByDependency[expression] ?: return super.visitCall(expression)
                    return irBlock {
                        val providerVar = irTemporaryVar(
                            irGetField(
                                irGet(invokeFunction.dispatchReceiverParameter!!),
                                fieldForDependency
                            )
                        )
                        +irIfThen(
                            irEqualsNull(irGet(providerVar)),
                            irBlock {
                                +irSetVar(
                                    providerVar.symbol,
                                    irCall(
                                        symbolTable.referenceFunction(
                                            component.unsubstitutedMemberScope
                                                .findSingleFunction(Name.identifier("getBindingProvider"))
                                        ),
                                        fieldForDependency.type.makeNotNull()
                                    ).apply {
                                        dispatchReceiver =
                                            irGet(invokeFunction.valueParameters.first())
                                        putTypeArgument(0, expression.getTypeArgument(0)!!)
                                        putValueArgument(0, expression.getValueArgument(0)!!)
                                    }
                                )
                                +irSetField(
                                    irGet(invokeFunction.dispatchReceiverParameter!!),
                                    fieldForDependency,
                                    irGet(providerVar)
                                )
                            }
                        )

                        +irCall(
                            fieldForDependency.type
                                .classOrNull!!
                                .getSimpleFunction("invoke")!!
                        ).apply {
                            dispatchReceiver = irGet(providerVar)
                            putValueArgument(
                                0,
                                irGet(invokeFunction.valueParameters.first())
                            )
                            putValueArgument(
                                1,
                                expression.getValueArgument(1) ?: irCall(
                                    symbolTable.referenceFunction(
                                        injektPackage
                                            .memberScope
                                            .findSingleFunction(Name.identifier("emptyParameters"))
                                    ),
                                    parameters.defaultType.toIrType()
                                )
                            )
                        }
                    }
                }
            })

            (invokeFunction.body as IrBlockBody).statements.add(
                0,
                resetFieldsExpression
            )
        }
    }
}
