package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstFunction<F : AstFunction<F>> : AstCallableDeclaration<F>, AstTargetElement, AstTypeParameterRefsOwner, AstStatement {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstAnnotationCall>
    override val returnTypeRef: AstTypeRef
    override val receiverTypeRef: AstTypeRef?
    override val typeParameters: List<AstTypeParameterRef>
    override val symbol: AstFunctionSymbol<F>
    val valueParameters: List<AstValueParameter>
    val body: AstBlock?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitFunction(this, data)

    override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef)

    override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?)

    fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstFunction<F>

    override fun <D> transformReturnTypeRef(transformer: AstTransformer<D>, data: D): AstFunction<F>

    override fun <D> transformReceiverTypeRef(transformer: AstTransformer<D>, data: D): AstFunction<F>

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstFunction<F>

    fun <D> transformValueParameters(transformer: AstTransformer<D>, data: D): AstFunction<F>

    fun <D> transformBody(transformer: AstTransformer<D>, data: D): AstFunction<F>
}
