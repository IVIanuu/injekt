package com.ivianuu.ast

import com.ivianuu.ast.visitors.CompositeTransformResult
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstElement {
    val context: AstContext

    fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitElement(this, data)

    fun accept(visitor: AstVisitorVoid) = accept(visitor, null)

    fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D)

    fun acceptChildren(visitor: AstVisitorVoid) = acceptChildren(visitor, null)

    @Suppress("UNCHECKED_CAST")
    fun <E : AstElement, D> transform(visitor: AstTransformer<D>, data: D): CompositeTransformResult<E> =
        accept(visitor, data) as CompositeTransformResult<E>

    fun <E : AstElement> transform(visitor: AstTransformerVoid): CompositeTransformResult<E> =
        transform(visitor, null)

    fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElement

    fun <E : AstElement> transformChildren(visitor: AstTransformerVoid): AstElement =
        transformChildren(visitor, null)
}
