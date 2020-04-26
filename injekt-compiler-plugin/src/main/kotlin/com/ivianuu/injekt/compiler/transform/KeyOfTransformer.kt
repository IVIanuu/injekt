package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.isFullyResolved
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization

class KeyOfTransformer(pluginContext: IrPluginContext) : AbstractInjektTransformer(pluginContext) {

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        val callee = expression.symbol.owner

        if ((callee.fqNameForIrSerialization.asString() != "com.ivianuu.injekt.keyOf" &&
                    callee.fqNameForIrSerialization.asString() != "com.ivianuu.injekt.KeyKt.keyOf") ||
            callee.valueParameters.size != 1 ||
            !callee.isInline
        ) return expression

        val type = expression.getTypeArgument(0)!!
        if (!type.isFullyResolved()) return expression

        return DeclarationIrBuilder(context, expression.symbol)
            .irKeyOf(type, expression.getValueArgument(0))
    }

    private fun IrBuilderWithScope.irKeyOf(
        type: IrType,
        qualifier: IrExpression? = null
    ): IrExpression {
        return if (type is IrSimpleType && type.arguments.isNotEmpty()) {
            val parameterizedKey = symbols.parameterizedKey

            irCall(
                parameterizedKey.constructors.single { it.valueParameters.size == 4 }
            ).apply {
                putTypeArgument(0, type)

                putValueArgument(
                    0,
                    IrClassReferenceImpl(
                        startOffset,
                        endOffset,
                        this@KeyOfTransformer.context.irBuiltIns.kClassClass.typeWith(type),
                        type.classifierOrFail,
                        type
                    )
                )

                putValueArgument(1, qualifier ?: irNull())

                putValueArgument(2, irBoolean(type.isMarkedNullable()))

                this@KeyOfTransformer.context.irBuiltIns.arrayClass
                    .typeWith(getTypeArgument(0)!!)
                val argumentsType = this@KeyOfTransformer.context.irBuiltIns.arrayClass
                    .typeWith(symbols.key.starProjectedType)

                putValueArgument(
                    3,
                    irCall(
                        this@KeyOfTransformer.context.symbols.arrayOf,
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
            }
        } else {
            irCall(symbols.simpleKey.constructors.single {
                it.valueParameters.size == 3
            }).apply {
                putTypeArgument(0, type)

                putValueArgument(
                    0,
                    IrClassReferenceImpl(
                        startOffset,
                        endOffset,
                        this@KeyOfTransformer.context.irBuiltIns.kClassClass.typeWith(type),
                        type.classifierOrFail,
                        type
                    )
                )

                putValueArgument(1, qualifier ?: irNull())

                putValueArgument(2, irBoolean(type.isMarkedNullable()))
            }
        }
    }
}
