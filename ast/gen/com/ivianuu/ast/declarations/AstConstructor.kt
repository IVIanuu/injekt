package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstConstructor : AstPureAbstractElement(), AstFunction<AstConstructor>, AstCallableMemberDeclaration<AstConstructor>, AstTypeParameterRefsOwner {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val returnTypeRef: AstTypeRef
    abstract override val receiverTypeRef: AstTypeRef?
    abstract override val typeParameters: List<AstTypeParameterRef>
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val status: AstDeclarationStatus
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val symbol: AstConstructorSymbol
    abstract val delegatedConstructor: AstDelegatedConstructorCall?
    abstract override val body: AstBlock?
    abstract val isPrimary: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitConstructor(this, data)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?)

    abstract override fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    abstract override fun <D> transformReturnTypeRef(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract override fun <D> transformReceiverTypeRef(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract override fun <D> transformValueParameters(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract fun <D> transformDelegatedConstructor(transformer: AstTransformer<D>, data: D): AstConstructor

    abstract override fun <D> transformBody(transformer: AstTransformer<D>, data: D): AstConstructor
}
