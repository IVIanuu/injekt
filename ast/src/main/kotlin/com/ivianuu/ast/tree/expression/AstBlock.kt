package com.ivianuu.ast.tree.expression

import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
import com.ivianuu.ast.tree.visitor.transformSingle

class AstBlock(override var type: AstType) : AstExpressionBase() {

    val statements: MutableList<AstStatement> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitBlock(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        statements.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        statements.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
    }

}
