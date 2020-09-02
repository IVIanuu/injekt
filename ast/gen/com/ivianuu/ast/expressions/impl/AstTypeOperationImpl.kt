package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.AstTypeOperation
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTypeOperationImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val operation: AstOperation,
) : AstTypeOperation() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeOperationImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        return this
    }
}
