package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstPrimaryConstructor(
    override val origin: AstDeclarationOrigin,
    override var returnTypeRef: AstTypeRef,
    override var receiverTypeRef: AstTypeRef?,
    override val typeParameters: MutableList<AstTypeParameterRef>,
    override val valueParameters: MutableList<AstValueParameter>,
    override var status: AstDeclarationStatus,
    override val annotations: MutableList<AstAnnotationCall>,
    override val symbol: AstConstructorSymbol,
    override var delegatedConstructor: AstDelegatedConstructorCall?,
    override var body: AstBlock?,
) : AstConstructor() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val isPrimary: Boolean get() = true

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        delegatedConstructor?.accept(visitor, data)
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        transformTypeParameters(transformer, data)
        transformValueParameters(transformer, data)
        transformStatus(transformer, data)
        transformAnnotations(transformer, data)
        transformDelegatedConstructor(transformer, data)
        transformBody(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDelegatedConstructor(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        delegatedConstructor = delegatedConstructor?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        body = body?.transformSingle(transformer, data)
        return this
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?) {
        receiverTypeRef = newReceiverTypeRef
    }

    override fun replaceValueParameters(newValueParameters: List<AstValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }
}
