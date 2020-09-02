package com.ivianuu.ast.expressions

import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstWhenExpression : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val subject: AstExpression?
    abstract val subjectVariable: AstVariable<*>?
    abstract val branches: List<AstWhenBranch>
    abstract val isExhaustive: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitWhenExpression(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract fun replaceIsExhaustive(newIsExhaustive: Boolean)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstWhenExpression

    abstract fun <D> transformSubject(transformer: AstTransformer<D>, data: D): AstWhenExpression

    abstract fun <D> transformBranches(transformer: AstTransformer<D>, data: D): AstWhenExpression

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstWhenExpression
}
