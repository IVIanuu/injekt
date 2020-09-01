package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstFunction<F : AstFunction<F>> : AstCallableDeclaration<F>, AstTargetElement, AstTypeParameterRefsOwner, AstStatement {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstAnnotationCall>
    override val returnType: AstType
    override val receiverType: AstType?
    override val typeParameters: List<AstTypeParameterRef>
    override val symbol: AstFunctionSymbol<F>
    val valueParameters: List<AstValueParameter>
    val body: AstBlock?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitFunction(this, data)

    override fun replaceReturnType(newReturnType: AstType)

    override fun replaceReceiverType(newReceiverType: AstType?)

    fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstFunction<F>

    override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstFunction<F>

    override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstFunction<F>

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstFunction<F>

    fun <D> transformValueParameters(transformer: AstTransformer<D>, data: D): AstFunction<F>

    fun <D> transformBody(transformer: AstTransformer<D>, data: D): AstFunction<F>
}
