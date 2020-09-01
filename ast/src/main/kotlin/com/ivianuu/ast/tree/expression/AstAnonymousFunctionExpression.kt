package com.ivianuu.ast.tree.expression

import com.ivianuu.ast.tree.declaration.AstFunction
import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformSingle

class AstAnonymousFunctionExpression(
    override var type: AstType,
    var anonymousFunction: AstFunction
) : AstExpressionBase() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitAnonymousFunctionExpression(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        anonymousFunction.accept(visitor, data)
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        anonymousFunction = anonymousFunction.transformSingle(transformer, data)
        type = type.transformSingle(transformer, data)
    }

}
