package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class GivenOptimizationTransformer : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        return if (expression.symbol.descriptor.fqNameSafe == InjektFqNames.givenfun) {
            expression.getValueArgument(0)!!
        } else super.visitCall(expression)
    }
}
