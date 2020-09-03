package com.ivianuu.ast.types

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstType : AstAnnotationContainer {
    override val annotations: List<AstFunctionCall>
    val isMarkedNullable: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitType(this, data)

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    fun replaceIsMarkedNullable(newIsMarkedNullable: Boolean)
}
