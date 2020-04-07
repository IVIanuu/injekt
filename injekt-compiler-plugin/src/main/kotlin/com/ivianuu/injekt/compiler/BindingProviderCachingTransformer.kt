package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irEqualsNull
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction

class BindingProviderCachingTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val component = getClass(InjektClassNames.Component)
    private val parameters = getClass(InjektClassNames.Parameters)

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (!declaration.symbol.isBound) pluginContext.irProvider.getDeclaration(declaration.symbol)
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

        classes.forEach {
            val pre = it.dump()
            transformClass(it)
            val post = it.dump()
            //if (pre != post) error("Transformed ${it.descriptor}\n\nPre:\n$pre\n\nPost:\n$post")
        }

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
                val descriptor = expression.symbol.descriptor
                if (descriptor.name.asString() == "get" &&
                    descriptor.dispatchReceiverParameter?.type == component.defaultType
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
                +irIfThen(
                    condition = irNot(
                        irEqeqeq(
                            irGetField(
                                irGet(invokeFunction.dispatchReceiverParameter!!),
                                boundComponentField
                            ),
                            irGet(invokeFunction.valueParameters.first())
                        )
                    ),
                    thenPart = irBlock {
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

            invokeFunction.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitCall(expression: IrCall): IrExpression {
                    val fieldForDependency =
                        fieldsByDependency[expression] ?: return super.visitCall(expression)
                    return irBlock {
                        +irIfThen(
                            irEqualsNull(
                                irGetField(
                                    irGet(invokeFunction.dispatchReceiverParameter!!),
                                    fieldForDependency
                                )
                            ),
                            irSetField(
                                irGet(invokeFunction.dispatchReceiverParameter!!),
                                fieldForDependency,
                                irCall(
                                    symbolTable.referenceFunction(
                                        component.unsubstitutedMemberScope
                                            .findSingleFunction(Name.identifier("getBindingProvider"))
                                    ),
                                    fieldForDependency.type.makeNotNull()
                                ).apply {
                                    dispatchReceiver = irGet(invokeFunction.valueParameters.first())
                                    putTypeArgument(0, expression.getTypeArgument(0)!!)
                                    putValueArgument(0, expression.getValueArgument(0)!!)
                                }
                            )
                        )

                        +irCall(
                            symbolTable.referenceFunction(
                                (fieldForDependency.type.toKotlinType()
                                    .constructor
                                    .declarationDescriptor as ClassDescriptor)
                                    .unsubstitutedMemberScope
                                    .findSingleFunction(Name.identifier("invoke"))
                            ),
                            (fieldForDependency.type as IrSimpleType).arguments.last()
                                .typeOrNull!!
                        ).apply {
                            dispatchReceiver = irGetField(
                                irGet(invokeFunction.dispatchReceiverParameter!!),
                                fieldForDependency
                            )
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
