package com.ivianuu.ast.types

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstTypeRef : AstAnnotationContainer {
    override val annotations: List<AstAnnotationCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTypeRef(this, data)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstTypeRef
}
