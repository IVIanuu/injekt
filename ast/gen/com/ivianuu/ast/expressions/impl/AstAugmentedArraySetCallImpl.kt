package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstAugmentedArraySetCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstAugmentedArraySetCallImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var assignCall: AstFunctionCall,
    override var setGetBlock: AstBlock,
    override val operation: AstOperation,
    override var calleeReference: AstReference,
) : AstAugmentedArraySetCall() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        assignCall.accept(visitor, data)
        setGetBlock.accept(visitor, data)
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstAugmentedArraySetCallImpl {
        transformAnnotations(transformer, data)
        assignCall = assignCall.transformSingle(transformer, data)
        setGetBlock = setGetBlock.transformSingle(transformer, data)
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstAugmentedArraySetCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceCalleeReference(newCalleeReference: AstReference) {
        calleeReference = newCalleeReference
    }
}
