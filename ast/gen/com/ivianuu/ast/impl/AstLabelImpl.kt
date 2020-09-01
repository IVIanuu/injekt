package com.ivianuu.ast.impl

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstLabelImpl(
    override val name: String,
) : AstLabel() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstLabelImpl {
        return this
    }
}
