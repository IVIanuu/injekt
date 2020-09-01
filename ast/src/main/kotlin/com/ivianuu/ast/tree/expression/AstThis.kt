package com.ivianuu.ast.tree.expression

import com.ivianuu.ast.tree.AstTarget
import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
import com.ivianuu.ast.tree.visitor.transformSingle

class AstThis(
    override var type: AstType,
    var target: AstTarget
) : AstExpressionBase() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitExpression(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
    }

}
