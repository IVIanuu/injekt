package com.ivianuu.ast.types.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

object AstTypePlaceholderProjection : AstTypeProjection() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypePlaceholderProjection {
        return this
    }
}
