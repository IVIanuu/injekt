package com.ivianuu.ast.tree.expression

import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
import com.ivianuu.ast.tree.visitor.transformSingle

class AstThrow(
    override var type: AstType,
    var expression: AstExpression
) : AstExpressionBase() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitThrow(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        expression.accept(visitor, data)
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        expression = expression.transformSingle(transformer, data)
        type = type.transformSingle(transformer, data)
    }

}
