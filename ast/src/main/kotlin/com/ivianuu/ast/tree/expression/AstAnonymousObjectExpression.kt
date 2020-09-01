package com.ivianuu.ast.tree.expression

import com.ivianuu.ast.tree.declaration.AstClass
import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformSingle

class AstAnonymousObjectExpression(
    override var type: AstType,
    var anonymousObject: AstClass
) : AstExpressionBase() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitAnonymousObjectExpression(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        anonymousObject.accept(visitor, data)
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        anonymousObject = anonymousObject.transformSingle(transformer, data)
        type = type.transformSingle(transformer, data)
    }

}
