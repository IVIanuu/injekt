package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstArgumentListImpl(
    override val arguments: MutableList<AstExpression>,
) : AstArgumentList() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstArgumentListImpl {
        transformArguments(transformer, data)
        return this
    }

    override fun <D> transformArguments(
        transformer: AstTransformer<D>,
        data: D
    ): AstArgumentListImpl {
        arguments.transformInplace(transformer, data)
        return this
    }
}
