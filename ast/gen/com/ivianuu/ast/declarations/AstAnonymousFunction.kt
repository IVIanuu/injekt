package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAnonymousFunction : AstFunction<AstAnonymousFunction>, AstExpression(), AstTypeParametersOwner {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val annotations: List<AstCall>
    abstract override val returnType: AstType
    abstract override val receiverType: AstType?
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val body: AstBlock?
    abstract override val type: AstType
    abstract override val symbol: AstAnonymousFunctionSymbol
    abstract val label: AstLabel?
    abstract val isLambda: Boolean
    abstract override val typeParameters: List<AstTypeParameter>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitAnonymousFunction(this, data)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract override fun replaceReceiverType(newReceiverType: AstType?)

    abstract override fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstAnonymousFunction

    abstract override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstAnonymousFunction

    abstract override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstAnonymousFunction

    abstract override fun <D> transformValueParameters(transformer: AstTransformer<D>, data: D): AstAnonymousFunction

    abstract override fun <D> transformBody(transformer: AstTransformer<D>, data: D): AstAnonymousFunction

    abstract override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstAnonymousFunction
}
