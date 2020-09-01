package com.ivianuu.ast

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstAnnotationContainer : AstElement {
    val annotations: List<AstAnnotationCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitAnnotationContainer(this, data)

    fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstAnnotationContainer
}
