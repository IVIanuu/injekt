package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.references.impl.AstExplicitSuperReference
import com.ivianuu.ast.references.impl.AstExplicitThisReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstDelegatedConstructorCallImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var argumentList: AstArgumentList,
    override var constructedType: AstType,
    override var dispatchReceiver: AstExpression,
    override val isThis: Boolean,
) : AstDelegatedConstructorCall() {
    override var calleeReference: AstReference = if (isThis) AstExplicitThisReference(null) else AstExplicitSuperReference(null, constructedType)
    override val isSuper: Boolean get() = !isThis

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
        constructedType.accept(visitor, data)
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstDelegatedConstructorCallImpl {
        transformAnnotations(transformer, data)
        argumentList = argumentList.transformSingle(transformer, data)
        constructedType = constructedType.transformSingle(transformer, data)
        transformCalleeReference(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstDelegatedConstructorCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDispatchReceiver(transformer: AstTransformer<D>, data: D): AstDelegatedConstructorCallImpl {
        dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: AstTransformer<D>, data: D): AstDelegatedConstructorCallImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun replaceArgumentList(newArgumentList: AstArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceConstructedType(newConstructedType: AstType) {
        constructedType = newConstructedType
    }

    override fun replaceCalleeReference(newCalleeReference: AstReference) {
        calleeReference = newCalleeReference
    }
}
