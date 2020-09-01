package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstStringConcatenationCall : AstCall, AstExpression() {
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val argumentList: AstArgumentList
    abstract override val typeRef: AstTypeRef

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitStringConcatenationCall(this, data)

    abstract override fun replaceArgumentList(newArgumentList: AstArgumentList)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstStringConcatenationCall
}
