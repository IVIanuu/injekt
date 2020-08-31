package com.ivianuu.injekt.compiler.ast.tree.expression

import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor

interface AstBreakContinue : AstExpression {
    var loop: AstLoop
}

class AstBreak(
    override var type: AstType,
    override var loop: AstLoop
) : AstExpressionBase(), AstBreakContinue {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitBreak(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
    }

}

class AstContinue(
    override var type: AstType,
    override var loop: AstLoop
) : AstExpressionBase(), AstBreakContinue {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitContinue(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
    }

}
