package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstStringConcatenationCall : AstCall, AstExpression() {
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val argumentList: AstArgumentList
    abstract override val type: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitStringConcatenationCall(this, data)

    abstract override fun replaceArgumentList(newArgumentList: AstArgumentList)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstStringConcatenationCall
}
