package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstCallableDeclaration<F : AstCallableDeclaration<F>> : AstDeclaration, AstSymbolOwner<F> {
    override val context: AstContext
    override val annotations: List<AstFunctionCall>
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    val dispatchReceiverType: AstType?
    val extensionReceiverType: AstType?
    val returnType: AstType
    override val symbol: AstCallableSymbol<F>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCallableDeclaration(this, data)

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    override fun replaceOrigin(newOrigin: AstDeclarationOrigin)

    override fun replaceAttributes(newAttributes: AstDeclarationAttributes)

    fun replaceDispatchReceiverType(newDispatchReceiverType: AstType?)

    fun replaceExtensionReceiverType(newExtensionReceiverType: AstType?)

    fun replaceReturnType(newReturnType: AstType)
}
