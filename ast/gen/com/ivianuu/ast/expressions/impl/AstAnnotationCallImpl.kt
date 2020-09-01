package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstAnnotationResolveStatus
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstAnnotationCallImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var argumentList: AstArgumentList,
    override var calleeReference: AstReference,
    override val useSiteTarget: AnnotationUseSiteTarget?,
    override var annotationTypeRef: AstTypeRef,
    override var resolveStatus: AstAnnotationResolveStatus,
) : AstAnnotationCall() {
    override val typeRef: AstTypeRef get() = annotationTypeRef

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
        calleeReference.accept(visitor, data)
        annotationTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnnotationCallImpl {
        transformAnnotations(transformer, data)
        argumentList = argumentList.transformSingle(transformer, data)
        transformCalleeReference(transformer, data)
        transformAnnotationTypeRef(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnnotationCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnnotationCallImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotationTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnnotationCallImpl {
        annotationTypeRef = annotationTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: AstTypeRef) {}

    override fun replaceArgumentList(newArgumentList: AstArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceCalleeReference(newCalleeReference: AstReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceResolveStatus(newResolveStatus: AstAnnotationResolveStatus) {
        resolveStatus = newResolveStatus
    }
}
