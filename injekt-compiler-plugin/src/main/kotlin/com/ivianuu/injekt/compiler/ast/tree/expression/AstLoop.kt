package com.ivianuu.injekt.compiler.ast.tree.expression

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformResult
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformSingle

interface AstLoop : AstExpression {
    var body: AstExpression?
    var condition: AstExpression
}

class AstWhileLoop(
    override var body: AstExpression?,
    override var condition: AstExpression,
    override var type: AstType
) : AstLoop {

    override val annotations: MutableList<AstQualifiedAccess> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitWhileLoop(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        condition.accept(visitor, data)
    }

    override fun <D> transform(
        transformer: AstTransformer<D>,
        data: D
    ): AstTransformResult<AstElement> =
        transformer.visitWhileLoop(this, data)

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        condition = condition.transformSingle(transformer, data)
    }

}

class AstDoWhileLoop(
    override var body: AstExpression?,
    override var condition: AstExpression,
    override var type: AstType
) : AstLoop {

    override val annotations: MutableList<AstQualifiedAccess> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitDoWhileLoop(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        condition.accept(visitor, data)
    }

    override fun <D> transform(
        transformer: AstTransformer<D>,
        data: D
    ): AstTransformResult<AstElement> =
        transformer.visitDoWhileLoop(this, data)

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        condition = condition.transformSingle(transformer, data)
    }

}
