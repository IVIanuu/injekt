package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstComponentCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.references.AstNamedReference
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.references.impl.AstSimpleNamedReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@OptIn(AstImplementationDetail::class)
internal class AstComponentCallImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override val typeArguments: MutableList<AstTypeProjection>,
    override var dispatchReceiver: AstExpression,
    override var extensionReceiver: AstExpression,
    override var argumentList: AstArgumentList,
    override var explicitReceiver: AstExpression,
    override val componentIndex: Int,
) : AstComponentCall() {
    override var type: AstType = AstImplicitTypeImpl()
    override var calleeReference: AstNamedReference = AstSimpleNamedReference(Name.identifier("component$componentIndex"), null)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
        calleeReference.accept(visitor, data)
        explicitReceiver.accept(visitor, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver.accept(visitor, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver.accept(visitor, data)
        }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstComponentCallImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        transformTypeArguments(transformer, data)
        argumentList = argumentList.transformSingle(transformer, data)
        transformCalleeReference(transformer, data)
        explicitReceiver = explicitReceiver.transformSingle(transformer, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver = extensionReceiver.transformSingle(transformer, data)
        }
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstComponentCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: AstTransformer<D>, data: D): AstComponentCallImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDispatchReceiver(transformer: AstTransformer<D>, data: D): AstComponentCallImpl {
        dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformExtensionReceiver(transformer: AstTransformer<D>, data: D): AstComponentCallImpl {
        extensionReceiver = extensionReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: AstTransformer<D>, data: D): AstComponentCallImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformExplicitReceiver(transformer: AstTransformer<D>, data: D): AstComponentCallImpl {
        explicitReceiver = explicitReceiver.transformSingle(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
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

    override fun replaceExplicitReceiver(newExplicitReceiver: AstExpression?) {
        require(newExplicitReceiver != null)
        explicitReceiver = newExplicitReceiver
    }
}
