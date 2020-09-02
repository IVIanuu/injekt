package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstTryExpression
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTryExpressionImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override var tryBlock: AstBlock,
    override val catches: MutableList<AstCatch>,
    override var finallyBlock: AstBlock?,
) : AstTryExpression() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        tryBlock.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finallyBlock?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTryExpressionImpl {
        transformOtherChildren(transformer, data)
        return this
    }
}
