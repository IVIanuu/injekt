package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.expressions.AstDelegateInitializer
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstDelegateInitializerImpl(
    override val context: AstContext,
    override var delegatedSuperType: AstType,
    override var expression: AstExpression,
) : AstDelegateInitializer() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        delegatedSuperType.accept(visitor, data)
        expression.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstDelegateInitializerImpl {
        delegatedSuperType = delegatedSuperType.transformSingle(transformer, data)
        expression = expression.transformSingle(transformer, data)
        return this
    }

    override fun replaceDelegatedSuperType(newDelegatedSuperType: AstType) {
        delegatedSuperType = newDelegatedSuperType
    }

    override fun replaceExpression(newExpression: AstExpression) {
        expression = newExpression
    }
}
