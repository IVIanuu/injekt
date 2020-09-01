package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.AstWhenExpression
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstWhenExpressionImpl(
    override var typeRef: AstTypeRef,
    override val annotations: MutableList<AstAnnotationCall>,
    override var calleeReference: AstReference,
    override var subject: AstExpression?,
    override var subjectVariable: AstVariable<*>?,
    override val branches: MutableList<AstWhenBranch>,
    override var isExhaustive: Boolean,
) : AstWhenExpression() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        calleeReference.accept(visitor, data)
        val subjectVariable_ = subjectVariable
        if (subjectVariable_ != null) {
            subjectVariable_.accept(visitor, data)
        } else {
            subject?.accept(visitor, data)
        }
        branches.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenExpressionImpl {
        transformCalleeReference(transformer, data)
        transformSubject(transformer, data)
        transformBranches(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenExpressionImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformSubject(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenExpressionImpl {
        if (subjectVariable != null) {
            subjectVariable = subjectVariable?.transformSingle(transformer, data)
            subject = subjectVariable?.initializer
        } else {
            subject = subject?.transformSingle(transformer, data)
        }
        return this
    }

    override fun <D> transformBranches(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenExpressionImpl {
        branches.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenExpressionImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: AstTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceCalleeReference(newCalleeReference: AstReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceIsExhaustive(newIsExhaustive: Boolean) {
        isExhaustive = newIsExhaustive
    }
}
