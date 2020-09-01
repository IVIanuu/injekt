package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTypeOperatorCall : AstExpression(), AstCall {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val argumentList: AstArgumentList
    abstract val operation: AstOperation
    abstract val conversionTypeRef: AstTypeRef

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTypeOperatorCall(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun replaceArgumentList(newArgumentList: AstArgumentList)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstTypeOperatorCall

    abstract fun <D> transformConversionTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstTypeOperatorCall

    abstract fun <D> transformOtherChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstTypeOperatorCall
}
