package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAnonymousFunction : AstFunction<AstAnonymousFunction>, AstExpression(),
    AstTypeParametersOwner {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val returnTypeRef: AstTypeRef
    abstract override val receiverTypeRef: AstTypeRef?
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val body: AstBlock?
    abstract override val typeRef: AstTypeRef
    abstract override val symbol: AstAnonymousFunctionSymbol
    abstract val label: AstLabel?
    abstract val isLambda: Boolean
    abstract override val typeParameters: List<AstTypeParameter>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitAnonymousFunction(this, data)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?)

    abstract override fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousFunction

    abstract override fun <D> transformReturnTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousFunction

    abstract override fun <D> transformReceiverTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousFunction

    abstract override fun <D> transformValueParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousFunction

    abstract override fun <D> transformBody(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousFunction

    abstract override fun <D> transformTypeParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousFunction
}
