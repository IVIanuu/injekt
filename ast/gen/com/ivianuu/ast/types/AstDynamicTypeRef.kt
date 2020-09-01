package com.ivianuu.ast.types

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstDynamicTypeRef : AstPureAbstractElement(), AstTypeRefWithNullability {
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val isMarkedNullable: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitDynamicTypeRef(this, data)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstDynamicTypeRef
}
