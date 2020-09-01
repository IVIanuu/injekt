package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstPropertyAccessor : AstPureAbstractElement(), AstFunction<AstPropertyAccessor>,
    AstTypeParametersOwner {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val returnTypeRef: AstTypeRef
    abstract override val receiverTypeRef: AstTypeRef?
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val body: AstBlock?
    abstract override val symbol: AstPropertyAccessorSymbol
    abstract val isGetter: Boolean
    abstract val isSetter: Boolean
    abstract val status: AstDeclarationStatus
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val typeParameters: List<AstTypeParameter>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitPropertyAccessor(this, data)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?)

    abstract override fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    abstract override fun <D> transformReturnTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessor

    abstract override fun <D> transformReceiverTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessor

    abstract override fun <D> transformValueParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessor

    abstract override fun <D> transformBody(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessor

    abstract fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstPropertyAccessor

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessor

    abstract override fun <D> transformTypeParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessor
}
