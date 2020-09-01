package com.ivianuu.ast.types.impl

import com.ivianuu.ast.types.AstStarProjection
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstStarProjectionImpl : AstStarProjection() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstStarProjectionImpl {
        return this
    }
}
