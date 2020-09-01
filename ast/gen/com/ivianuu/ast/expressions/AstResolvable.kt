package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstResolvable : AstElement {
    val calleeReference: AstReference

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitResolvable(this, data)

    fun replaceCalleeReference(newCalleeReference: AstReference)

    fun <D> transformCalleeReference(transformer: AstTransformer<D>, data: D): AstResolvable
}
