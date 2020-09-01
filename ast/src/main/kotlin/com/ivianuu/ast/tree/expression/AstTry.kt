package com.ivianuu.ast.tree.expression

import com.ivianuu.ast.tree.AstElement
import com.ivianuu.ast.tree.declaration.AstValueParameter
import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
import com.ivianuu.ast.tree.visitor.transformSingle

class AstTry(
    override var type: AstType,
    var tryResult: AstExpression,
    val catches: MutableList<AstCatch> = mutableListOf(),
    var finally: AstExpression? = null
) : AstExpressionBase() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTry(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        tryResult.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finally?.accept(visitor, data)
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        tryResult = tryResult.transformSingle(transformer, data)
        catches.transformInplace(transformer, data)
        finally = finally?.transformSingle(transformer, data)
        type = type.transformSingle(transformer, data)
    }

}

class AstCatch(
    var catchParameter: AstValueParameter,
    var result: AstExpression
) : AstElement {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitCatch(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        catchParameter.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        catchParameter = catchParameter.transformSingle(transformer, data)
        result = result.transformSingle(transformer, data)
    }

}
