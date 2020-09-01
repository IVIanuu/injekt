package com.ivianuu.ast

import com.ivianuu.ast.expressions.AstExpression

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class AstContractViolation

/**
 * This class is created to implicitly overcome FIR contract that some node may only be referenced once from its parents (not from any other node)
 * Thus, these kinds of secondary references may not be common FIR nodes since it would lead to visiting them twice during usual traversal
 * And this class is used to wrap such references
 */
class AstExpressionRef<T : AstExpression> @AstContractViolation constructor() {
    lateinit var value: T
    fun bind(value: T) {
        this.value = value
    }
}
