package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization

class KeyOfTransformer(pluginContext: IrPluginContext) : AbstractInjektTransformer(pluginContext) {

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

        return DeclarationIrBuilder(pluginContext, expression.symbol).irKeyOf(
            type, expression.getValueArgument(0)
        )
    }

}
