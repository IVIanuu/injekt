package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.cast

class KeyOfTransformer(pluginContext: IrPluginContext) : AbstractInjektTransformer(pluginContext) {

    private val key = getClass(InjektClassNames.Key)
    private val qualifier = getClass(InjektClassNames.Qualifier)

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        val callee = expression.symbol.ensureBound().owner

        if ((callee.fqNameForIrSerialization.asString() != "com.ivianuu.injekt.keyOf" &&
                    callee.fqNameForIrSerialization.asString() != "com.ivianuu.injekt.KeyKt.keyOf") ||
            callee.valueParameters.size != 1 ||
            !callee.isInline
        ) return expression

        val type = expression.getTypeArgument(0)!!
        if (!type.isFullyResolved()) return expression

        return DeclarationIrBuilder(pluginContext, expression.symbol)
            .irKeyOf(type, expression.getValueArgument(0))
    }

    private fun IrBuilderWithScope.irKeyOf(
        type: IrType,
        qualifier: IrExpression? = null
    ): IrExpression {
        val kotlinType = type.toKotlinType()

        return if (kotlinType.arguments.isNotEmpty()) {
            val parameterizedKey = key.sealedSubclasses
                .single { it.name.asString() == "ParameterizedKey" }

            irCall(
                symbolTable.referenceConstructor(parameterizedKey.constructors.first {
                    it.valueParameters.size == if (qualifier != null) 4 else 4
                }),
                KotlinTypeFactory.simpleType(
                    baseType = parameterizedKey.defaultType,
                    arguments = listOf(type.toKotlinType().asTypeProjection())
                ).toIrType()
            ).apply {
                putTypeArgument(0, type)

                putValueArgument(
                    0,
                    IrClassReferenceImpl(
                        startOffset,
                        endOffset,
                        pluginContext.irBuiltIns.kClassClass.typeWith(type),
                        type.classifierOrFail,
                        type
                    )
                )

                putValueArgument(
                    1, qualifier ?: irGetObject(
                        symbolTable.referenceClass(
                            this@KeyOfTransformer.qualifier.unsubstitutedMemberScope
                                .getContributedClassifier(
                                    Name.identifier("None"),
                                    NoLookupLocation.FROM_BACKEND
                                )
                                .cast()
                        )
                    )
                )

                putValueArgument(2, irBoolean(type.isMarkedNullable()))

                pluginContext.irBuiltIns.arrayClass
                    .typeWith(getTypeArgument(0)!!)
                val argumentsType = pluginContext.irBuiltIns.arrayClass
                    .typeWith(symbolTable.referenceClass(key).starProjectedType)

                putValueArgument(
                    3,
                    irCall(
                        pluginContext.symbols.arrayOf,
                        argumentsType
                    ).apply {
                        putValueArgument(
                            0,
                            IrVarargImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                argumentsType,
                                argumentsType,
                                type.toKotlinType().arguments
                                    .map { irKeyOf(it.type.toIrType()) }
                            )
                        )
                    }
                )

                /*if (qualifier == null) {
                    val hashCode = kotlinType.constructor.declarationDescriptor!!.fqNameSafe.toString().hashCode()
                    var hashCode = kotlinType.constructor.declarationDescriptor!!.fqNameSafe.toString().hashCode()
                    hashCode = 31 * hashCode + "Qualifier.None".hashCode()
                    putValueArgument(3, irInt(hashCode))
                    putValueArgument(4, irInt(hashCode))
                }*/
            }
        } else {
            val simpleKey = key.sealedSubclasses
                .single { it.name.asString() == "SimpleKey" }

            irCall(
                symbolTable.referenceConstructor(simpleKey.constructors.first {
                    it.valueParameters.size == if (qualifier != null) 3 else 4
                }),
                KotlinTypeFactory.simpleType(
                    baseType = simpleKey.defaultType,
                    arguments = listOf(type.toKotlinType().asTypeProjection())
                ).toIrType()
            ).apply {
                putTypeArgument(0, type)

                putValueArgument(
                    0,
                    IrClassReferenceImpl(
                        startOffset,
                        endOffset,
                        pluginContext.irBuiltIns.kClassClass.typeWith(type),
                        type.classifierOrFail,
                        type
                    )
                )

                putValueArgument(
                    1, qualifier ?: irGetObject(
                        symbolTable.referenceClass(
                            this@KeyOfTransformer.qualifier.unsubstitutedMemberScope
                                .getContributedClassifier(
                                    Name.identifier("None"),
                                    NoLookupLocation.FROM_BACKEND
                                )
                                .cast()
                        )
                    )
                )

                putValueArgument(2, irBoolean(type.isMarkedNullable()))

                if (qualifier == null) {
                    var hashCode =
                        kotlinType.constructor.declarationDescriptor!!.fqNameSafe.toString()
                            .hashCode()
                    hashCode = 31 * hashCode + "Qualifier.None".hashCode()
                    putValueArgument(3, irInt(hashCode))
                }
            }
        }
    }
}
