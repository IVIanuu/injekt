package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstConstExpression<T> : AstExpression() {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract val kind: AstConstKind<T>
    abstract val value: T

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitConstExpression(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract fun replaceKind(newKind: AstConstKind<T>)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstConstExpression<T>
}
