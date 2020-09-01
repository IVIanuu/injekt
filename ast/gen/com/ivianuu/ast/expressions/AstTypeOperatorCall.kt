package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTypeOperatorCall : AstExpression(), AstCall {
    abstract override val type: AstType
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val argumentList: AstArgumentList
    abstract val operation: AstOperation
    abstract val conversionType: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypeOperatorCall(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun replaceArgumentList(newArgumentList: AstArgumentList)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstTypeOperatorCall

    abstract fun <D> transformConversionType(transformer: AstTransformer<D>, data: D): AstTypeOperatorCall

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstTypeOperatorCall
}
