package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstFunction<F : AstFunction<F>> : AstCallableDeclaration<F>, AstTargetElement {
    override val annotations: List<AstFunctionCall>
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val receiverType: AstType?
    override val returnType: AstType
    override val symbol: AstFunctionSymbol<F>
    val valueParameters: List<AstValueParameter>
    val body: AstBlock?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitFunction(this, data)

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    override fun replaceReceiverType(newReceiverType: AstType?)

    override fun replaceReturnType(newReturnType: AstType)

    fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    fun replaceBody(newBody: AstBlock?)
}
