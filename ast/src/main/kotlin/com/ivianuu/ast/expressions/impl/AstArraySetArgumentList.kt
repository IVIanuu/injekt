package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAbstractArgumentList
import com.ivianuu.ast.expressions.AstExpression

class AstArraySetArgumentList internal constructor(
    private val rValue: AstExpression,
    private val indexes: List<AstExpression>
) : AstAbstractArgumentList() {
    override val arguments: List<AstExpression>
        get() = indexes + rValue
}
