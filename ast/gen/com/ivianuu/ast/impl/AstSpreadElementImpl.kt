package com.ivianuu.ast.impl

import com.ivianuu.ast.AstSpreadElement
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstSpreadElementImpl : AstSpreadElement() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstSpreadElementImpl {
        return this
    }
}
