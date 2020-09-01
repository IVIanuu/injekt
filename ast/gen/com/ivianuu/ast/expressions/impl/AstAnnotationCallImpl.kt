package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstAnnotationCallImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var argumentList: AstArgumentList,
    override var calleeReference: AstReference,
    override var annotationType: AstType,
) : AstAnnotationCall() {
    override val type: AstType get() = annotationType

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
        calleeReference.accept(visitor, data)
        annotationType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstAnnotationCallImpl {
        transformAnnotations(transformer, data)
        argumentList = argumentList.transformSingle(transformer, data)
        transformCalleeReference(transformer, data)
        transformAnnotationType(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstAnnotationCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: AstTransformer<D>, data: D): AstAnnotationCallImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotationType(transformer: AstTransformer<D>, data: D): AstAnnotationCallImpl {
        annotationType = annotationType.transformSingle(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {}

    override fun replaceArgumentList(newArgumentList: AstArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceCalleeReference(newCalleeReference: AstReference) {
        calleeReference = newCalleeReference
    }
}
