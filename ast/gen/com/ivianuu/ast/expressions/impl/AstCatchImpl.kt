package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstCatchImpl(
    override val context: AstContext,
    override var parameter: AstProperty,
    override var body: AstExpression,
) : AstCatch() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        parameter.accept(visitor, data)
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstCatchImpl {
        parameter = parameter.transformSingle(transformer, data)
        body = body.transformSingle(transformer, data)
        return this
    }

    override fun replaceParameter(newParameter: AstProperty) {
        parameter = newParameter
    }

    override fun replaceBody(newBody: AstExpression) {
        body = newBody
    }
}
