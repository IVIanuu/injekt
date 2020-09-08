package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstConstructor : AstPureAbstractElement(), AstFunction<AstConstructor>, AstCallableDeclaration<AstConstructor> {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val dispatchReceiverType: AstType?
    abstract override val extensionReceiverType: AstType?
    abstract override val returnType: AstType
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val symbol: AstConstructorSymbol
    abstract val delegatedConstructor: AstDelegatedConstructorCall?
    abstract override val body: AstBlock?
    abstract val visibility: Visibility
    abstract val isPrimary: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitConstructor(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceOrigin(newOrigin: AstDeclarationOrigin)

    abstract override fun replaceAttributes(newAttributes: AstDeclarationAttributes)

    abstract override fun replaceDispatchReceiverType(newDispatchReceiverType: AstType?)

    abstract override fun replaceExtensionReceiverType(newExtensionReceiverType: AstType?)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract override fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    abstract fun replaceDelegatedConstructor(newDelegatedConstructor: AstDelegatedConstructorCall?)

    abstract override fun replaceBody(newBody: AstBlock?)

    abstract fun replaceVisibility(newVisibility: Visibility)

    abstract fun replaceIsPrimary(newIsPrimary: Boolean)
}
