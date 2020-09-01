package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.references.AstNamedReference
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class AstFunctionCallImpl @AstImplementationDetail constructor(
    override var typeRef: AstTypeRef,
    override val annotations: MutableList<AstAnnotationCall>,
    override val typeArguments: MutableList<AstTypeProjection>,
    override var explicitReceiver: AstExpression?,
    override var dispatchReceiver: AstExpression,
    override var extensionReceiver: AstExpression,
    override var argumentList: AstArgumentList,
    override var calleeReference: AstNamedReference,
) : AstFunctionCall() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
        explicitReceiver?.accept(visitor, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver.accept(visitor, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver.accept(visitor, data)
        }
        argumentList.accept(visitor, data)
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstFunctionCallImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        transformTypeArguments(transformer, data)
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver = extensionReceiver.transformSingle(transformer, data)
        }
        argumentList = argumentList.transformSingle(transformer, data)
        transformCalleeReference(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstFunctionCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(
        transformer: AstTransformer<D>,
        data: D
    ): AstFunctionCallImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformExplicitReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstFunctionCallImpl {
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformDispatchReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstFunctionCallImpl {
        dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformExtensionReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstFunctionCallImpl {
        extensionReceiver = extensionReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(
        transformer: AstTransformer<D>,
        data: D
    ): AstFunctionCallImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: AstTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }

    override fun replaceExplicitReceiver(newExplicitReceiver: AstExpression?) {
        explicitReceiver = newExplicitReceiver
    }

    override fun replaceArgumentList(newArgumentList: AstArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceCalleeReference(newCalleeReference: AstNamedReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceCalleeReference(newCalleeReference: AstReference) {
        require(newCalleeReference is AstNamedReference)
        replaceCalleeReference(newCalleeReference)
    }
}
