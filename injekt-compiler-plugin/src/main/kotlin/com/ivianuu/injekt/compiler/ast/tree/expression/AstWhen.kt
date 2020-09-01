package com.ivianuu.injekt.compiler.ast.tree.expression

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformSingle

class AstWhen(
    override var type: AstType
) : AstExpressionBase() {

    val branches: MutableList<AstBranch> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitWhen(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        branches.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        branches.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
    }

}

interface AstBranch : AstElement {
    var result: AstExpression
}

abstract class AstBranchBase : AstBranch {

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        result = result.transformSingle(transformer, data)
    }

}

class AstConditionBranch(
    var condition: AstExpression,
    override var result: AstExpression
) : AstBranchBase() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitConditionBranch(this, data)

}

class AstElseBranch(override var result: AstExpression) : AstBranchBase() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitElseBranch(this, data)

}
