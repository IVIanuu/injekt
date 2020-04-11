package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irEqualsNull
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irSetVar
import org.jetbrains.kotlin.ir.builders.irTemporaryVar
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction

class BindingProviderCachingTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val component = getClass(InjektClassNames.Component)
    private val key = getClass(InjektClassNames.Key)
    private val parameters = getClass(InjektClassNames.Parameters)

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.modality != Modality.ABSTRACT &&
                    declaration.superTypes.any { superType ->
                        val kotlinType = superType.toKotlinType()
                        kotlinType.isFunctionType &&
                                kotlinType.arguments.size == 3 &&
                                kotlinType.arguments[0].type == component.defaultType &&
                                kotlinType.arguments[1].type == parameters.defaultType
                    }
                ) {
                    classes += declaration
                }

                return super.visitClass(declaration)
            }
        })

        classes.forEach { transformClass(it) }

        return super.visitModuleFragment(declaration)
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
                            key
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
