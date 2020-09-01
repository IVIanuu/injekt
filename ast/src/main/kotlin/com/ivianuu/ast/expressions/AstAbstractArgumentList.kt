package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

abstract class AstAbstractArgumentList : AstArgumentList() {
    override fun <D> transformArguments(transformer: AstTransformer<D>, data: D): AstArgumentList {
        return this
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        // DO NOTHING
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElement {
        return this
    }
}