package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAbstractArgumentList
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.visitors.AstVisitor

class AstResolvedArgumentList internal constructor(
    val mapping: Map<AstExpression, AstValueParameter>
) : AstAbstractArgumentList() {
    override val arguments: List<AstExpression>
        get() = mapping.keys.toList()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        for (argument in mapping.keys) {
            argument.accept(visitor, data)
        }
    }
}
