package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstStatement : AstAnnotationContainer {
    override val annotations: List<AstFunctionCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitStatement(this, data)

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)
}
