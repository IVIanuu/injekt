package com.ivianuu.ast.types

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstTypeRefWithNullability : AstTypeRef {
    override val annotations: List<AstAnnotationCall>
    val isMarkedNullable: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypeRefWithNullability(this, data)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstTypeRefWithNullability
}
