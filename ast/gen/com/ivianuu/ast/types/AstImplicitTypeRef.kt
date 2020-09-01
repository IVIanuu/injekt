package com.ivianuu.ast.types

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstImplicitTypeRef : AstPureAbstractElement(), AstTypeRef {
    abstract override val annotations: List<AstAnnotationCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitImplicitTypeRef(this, data)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstImplicitTypeRef
}
