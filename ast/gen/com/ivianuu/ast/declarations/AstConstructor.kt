package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstConstructor : AstPureAbstractElement(), AstFunction<AstConstructor>, AstCallableMemberDeclaration<AstConstructor> {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val returnType: AstType
    abstract override val receiverType: AstType?
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val status: AstDeclarationStatus
    abstract override val annotations: List<AstFunctionCall>
    abstract override val symbol: AstConstructorSymbol
    abstract val delegatedConstructor: AstDelegatedConstructorCall?
    abstract override val body: AstBlock?
    abstract val isPrimary: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitConstructor(this, data)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract override fun replaceReceiverType(newReceiverType: AstType?)

    abstract override fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    abstract override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract override fun <D> transformValueParameters(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract fun <D> transformDelegatedConstructor(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract override fun <D> transformBody(transformer: AstTransformer<D>, data: D): AstConstructor
}
