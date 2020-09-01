package com.ivianuu.ast.tree.expression

import com.ivianuu.ast.tree.AstElement
import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
import com.ivianuu.ast.tree.visitor.transformSingle

interface AstVarargElement : AstElement

class AstVararg(override var type: AstType) : AstExpressionBase() {

    override val annotations: MutableList<AstQualifiedAccess> = mutableListOf()
    val elements: MutableList<AstVarargElement> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitVararg(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        elements.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        elements.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
    }

}

class AstSpreadElement(var expression: AstExpression) : AstElement, AstVarargElement {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitSpreadElement(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        expression.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        expression = expression.transformSingle(transformer, data)
    }

}
