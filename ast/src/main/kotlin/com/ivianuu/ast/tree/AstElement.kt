package com.ivianuu.ast.tree

import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor

interface AstElement {

    fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R
    fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
    }

    fun <D> transform(transformer: AstTransformer<D>, data: D) =
        accept(transformer, data)

    fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
    }

}
