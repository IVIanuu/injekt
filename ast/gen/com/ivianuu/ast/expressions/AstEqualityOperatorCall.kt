package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstEqualityOperatorCall : AstExpression(), AstCall {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val argumentList: AstArgumentList
    abstract val operation: AstOperation

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitEqualityOperatorCall(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun replaceArgumentList(newArgumentList: AstArgumentList)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstEqualityOperatorCall
}
