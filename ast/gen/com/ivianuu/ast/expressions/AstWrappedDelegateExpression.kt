package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstWrappedDelegateExpression : AstWrappedExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val expression: AstExpression
    abstract val delegateProvider: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitWrappedDelegateExpression(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstWrappedDelegateExpression
}
