package com.ivianuu.ast.expressions

import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAnnotationCall : AstExpression(), AstCall, AstResolvable {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val argumentList: AstArgumentList
    abstract override val calleeReference: AstReference
    abstract val useSiteTarget: AnnotationUseSiteTarget?
    abstract val annotationTypeRef: AstTypeRef
    abstract val resolveStatus: AstAnnotationResolveStatus

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitAnnotationCall(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun replaceArgumentList(newArgumentList: AstArgumentList)

    abstract override fun replaceCalleeReference(newCalleeReference: AstReference)

    abstract fun replaceResolveStatus(newResolveStatus: AstAnnotationResolveStatus)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnnotationCall

    abstract override fun <D> transformCalleeReference(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnnotationCall

    abstract fun <D> transformAnnotationTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnnotationCall
}
