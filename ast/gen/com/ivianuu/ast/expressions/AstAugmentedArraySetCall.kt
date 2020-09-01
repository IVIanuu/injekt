package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAugmentedArraySetCall : AstPureAbstractElement(), AstStatement {
    abstract override val annotations: List<AstAnnotationCall>
    abstract val assignCall: AstFunctionCall
    abstract val setGetBlock: AstBlock
    abstract val operation: AstOperation
    abstract val calleeReference: AstReference

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitAugmentedArraySetCall(this, data)

    abstract fun replaceCalleeReference(newCalleeReference: AstReference)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstAugmentedArraySetCall
}
