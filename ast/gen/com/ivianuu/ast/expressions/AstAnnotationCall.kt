package com.ivianuu.ast.expressions

import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAnnotationCall : AstExpression(), AstCall, AstResolvable {
    abstract override val type: AstType
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val argumentList: AstArgumentList
    abstract override val calleeReference: AstReference
    abstract val annotationType: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitAnnotationCall(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun replaceArgumentList(newArgumentList: AstArgumentList)

    abstract override fun replaceCalleeReference(newCalleeReference: AstReference)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstAnnotationCall

    abstract override fun <D> transformCalleeReference(transformer: AstTransformer<D>, data: D): AstAnnotationCall

    abstract fun <D> transformAnnotationType(transformer: AstTransformer<D>, data: D): AstAnnotationCall
}
