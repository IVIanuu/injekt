package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class AstPropertyAccessorImpl @AstImplementationDetail constructor(
    override val origin: AstDeclarationOrigin,
    override var returnTypeRef: AstTypeRef,
    override val valueParameters: MutableList<AstValueParameter>,
    override var body: AstBlock?,
    override val symbol: AstPropertyAccessorSymbol,
    override val isGetter: Boolean,
    override var status: AstDeclarationStatus,
    override val annotations: MutableList<AstAnnotationCall>,
    override val typeParameters: MutableList<AstTypeParameter>,
) : AstPropertyAccessor() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val receiverTypeRef: AstTypeRef? get() = null
    override val isSetter: Boolean get() = !isGetter

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        status.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessorImpl {
        transformReturnTypeRef(transformer, data)
        transformValueParameters(transformer, data)
        transformBody(transformer, data)
        transformStatus(transformer, data)
        transformAnnotations(transformer, data)
        transformTypeParameters(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessorImpl {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessorImpl {
        return this
    }

    override fun <D> transformValueParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessorImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBody(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessorImpl {
        body = body?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformStatus(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessorImpl {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessorImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyAccessorImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?) {}

    override fun replaceValueParameters(newValueParameters: List<AstValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }
}
