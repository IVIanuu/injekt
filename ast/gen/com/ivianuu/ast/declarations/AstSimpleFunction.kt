package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstSimpleFunction : AstPureAbstractElement(), AstFunction<AstSimpleFunction>, AstCallableMemberDeclaration<AstSimpleFunction>, AstTypeParametersOwner {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val returnType: AstType
    abstract override val receiverType: AstType?
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val body: AstBlock?
    abstract override val status: AstDeclarationStatus
    abstract val name: Name
    abstract override val symbol: AstFunctionSymbol<AstSimpleFunction>
    abstract override val annotations: List<AstFunctionCall>
    abstract override val typeParameters: List<AstTypeParameter>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitSimpleFunction(this, data)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract override fun replaceReceiverType(newReceiverType: AstType?)

    abstract override fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    abstract override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstSimpleFunction

    abstract override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstSimpleFunction

    abstract override fun <D> transformValueParameters(transformer: AstTransformer<D>, data: D): AstSimpleFunction

    abstract override fun <D> transformBody(transformer: AstTransformer<D>, data: D): AstSimpleFunction

    abstract override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstSimpleFunction

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstSimpleFunction

    abstract override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstSimpleFunction
}
