package com.ivianuu.ast.tree.expression

import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
import com.ivianuu.ast.tree.visitor.transformSingle

abstract class AstBinaryOperation : AstExpressionBase() {

    abstract var left: AstExpression
    abstract var right: AstExpression

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        left.accept(visitor, data)
        right.accept(visitor, data)
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        left = left.transformSingle(transformer, data)
        right = right.transformSingle(transformer, data)
        type = type.transformSingle(transformer, data)
    }

}

class AstComparisonOperation(
    override var type: AstType,
    var kind: Kind,
    override var left: AstExpression,
    override var right: AstExpression
) : AstBinaryOperation() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitComparisonOperation(this, data)

    enum class Kind {
        LESS_THAN,
        GREATER_THAN,
        LESS_THEN_EQUALS,
        GREATER_THEN_EQUALS,
    }

}

class AstEqualityOperation(
    override var type: AstType,
    var kind: Kind,
    override var left: AstExpression,
    override var right: AstExpression
) : AstBinaryOperation() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitEqualityOperation(this, data)

    enum class Kind {
        EQUALS,
        NOT_EQUALS,
        IDENTITY,
        NOT_IDENTITY
    }

}

class AstLogicOperation(
    override var type: AstType,
    var kind: Kind,
    override var left: AstExpression,
    override var right: AstExpression
) : AstBinaryOperation() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitLogicOperation(this, data)

    enum class Kind {
        AND, OR
    }

}
