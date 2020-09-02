package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstExpressionRef
import com.ivianuu.ast.expressions.AstCheckedSafeCallSubject
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstSafeCallExpression
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstSafeCallExpressionImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override var receiver: AstExpression,
    override val checkedSubjectRef: AstExpressionRef<AstCheckedSafeCallSubject>,
    override var regularQualifiedAccess: AstQualifiedAccess,
) : AstSafeCallExpression() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        receiver.accept(visitor, data)
        regularQualifiedAccess.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstSafeCallExpressionImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        receiver = receiver.transformSingle(transformer, data)
        regularQualifiedAccess = regularQualifiedAccess.transformSingle(transformer, data)
        return this
    }
}
