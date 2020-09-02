package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstDelegatedConstructorCall : AstPureAbstractElement(), AstCall {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val valueArguments: List<AstExpression>
    abstract val constructedType: AstType
    abstract val dispatchReceiver: AstExpression?
    abstract val isThis: Boolean
    abstract val isSuper: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDelegatedConstructorCall(this, data)

    abstract fun replaceConstructedType(newConstructedType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstDelegatedConstructorCall

    abstract fun <D> transformDispatchReceiver(transformer: AstTransformer<D>, data: D): AstDelegatedConstructorCall
}
