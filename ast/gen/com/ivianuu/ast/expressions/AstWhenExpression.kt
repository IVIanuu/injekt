package com.ivianuu.ast.expressions

import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstWhenExpression : AstExpression(), AstResolvable {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val calleeReference: AstReference
    abstract val subject: AstExpression?
    abstract val subjectVariable: AstVariable<*>?
    abstract val branches: List<AstWhenBranch>
    abstract val isExhaustive: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitWhenExpression(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun replaceCalleeReference(newCalleeReference: AstReference)

    abstract fun replaceIsExhaustive(newIsExhaustive: Boolean)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenExpression

    abstract override fun <D> transformCalleeReference(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenExpression

    abstract fun <D> transformSubject(transformer: AstTransformer<D>, data: D): AstWhenExpression

    abstract fun <D> transformBranches(transformer: AstTransformer<D>, data: D): AstWhenExpression

    abstract fun <D> transformOtherChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenExpression
}
