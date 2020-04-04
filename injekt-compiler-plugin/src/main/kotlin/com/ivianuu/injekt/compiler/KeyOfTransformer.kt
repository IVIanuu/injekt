package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
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
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class KeyOfTransformer(pluginContext: IrPluginContext) : AbstractInjektTransformer(pluginContext) {

    private val key = getClass(InjektClassNames.Key)

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        val descriptor = expression.symbol.descriptor
        if (descriptor.fqNameSafe.asString() != "com.ivianuu.injekt.keyOf" ||
            descriptor.valueParameters.size != 1 ||
            !descriptor.isInline
        ) return expression

        val type = expression.getTypeArgument(0)!!

        if (!type.toKotlinType().isFullyResolved()) {
            message("Not resolved ${type.toKotlinType()}")
            return expression
        }

        message("Resolved ${type.toKotlinType()}")

        return irKeyOf(type, DeclarationIrBuilder(pluginContext, expression.symbol)).apply {
            // pass the qualifier
            putValueArgument(3, expression.getValueArgument(0))
        }
    }


    private fun irKeyOf(type: IrType, builder: DeclarationIrBuilder): IrCall {
        val keyOf = injektPackage.memberScope
            .findFirstFunction("keyOf") {
                it.valueParameters.size == 4
            }

        return builder.irCall(
            symbolTable.referenceSimpleFunction(keyOf),
            KotlinTypeFactory.simpleType(
                baseType = key.defaultType,
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

            if (type.isMarkedNullable()) {
                putValueArgument(1, builder.irBoolean(true))
            }

            if (type.toKotlinType().arguments.isNotEmpty()) {
                pluginContext.irBuiltIns.arrayClass
                    .typeWith(getTypeArgument(0)!!)
                val argumentsType = pluginContext.irBuiltIns.arrayClass
                    .typeWith(
                        symbolTable.referenceClass(key)
                            .also {
                                if (!it.isBound) pluginContext.irProvider.getDeclaration(it)
                            }
                            .starProjectedType
                    )

                putValueArgument(
                    2,
                    builder.irCall(
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
                                    .map { irKeyOf(it.type.toIrType(), builder) }
                            )
                        )
                    }
                )
            }

        }
    }
}
