package com.ivianuu.ast

import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstAnnotationContainer : AstElement {
    override val context: AstContext
    val annotations: List<AstFunctionCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitAnnotationContainer(this, data)

    fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)
}
