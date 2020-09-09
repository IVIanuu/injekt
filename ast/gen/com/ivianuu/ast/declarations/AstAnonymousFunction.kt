package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAnonymousFunction : AstPureAbstractElement(), AstFunction<AstAnonymousFunction>, AstExpression {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val dispatchReceiverType: AstType?
    abstract override val extensionReceiverType: AstType?
    abstract override val returnType: AstType
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val body: AstBlock?
    abstract override val type: AstType
    abstract override val symbol: AstAnonymousFunctionSymbol

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitAnonymousFunction(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceOrigin(newOrigin: AstDeclarationOrigin)

    abstract override fun replaceAttributes(newAttributes: AstDeclarationAttributes)

    abstract override fun replaceDispatchReceiverType(newDispatchReceiverType: AstType?)

    abstract override fun replaceExtensionReceiverType(newExtensionReceiverType: AstType?)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract override fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    abstract override fun replaceBody(newBody: AstBlock?)

    abstract override fun replaceType(newType: AstType)
}
