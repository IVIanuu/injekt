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
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstConstructorImpl(
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override var receiverType: AstType?,
    override val typeParameters: MutableList<AstTypeParameterRef>,
    override val valueParameters: MutableList<AstValueParameter>,
    override var status: AstDeclarationStatus,
    override val annotations: MutableList<AstAnnotationCall>,
    override val symbol: AstConstructorSymbol,
    override var delegatedConstructor: AstDelegatedConstructorCall?,
    override var body: AstBlock?,
) : AstConstructor() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val isPrimary: Boolean get() = false

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnType.accept(visitor, data)
        receiverType?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        delegatedConstructor?.accept(visitor, data)
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstConstructorImpl {
        transformReturnType(transformer, data)
        transformReceiverType(transformer, data)
        transformTypeParameters(transformer, data)
        transformValueParameters(transformer, data)
        transformStatus(transformer, data)
        transformAnnotations(transformer, data)
        transformDelegatedConstructor(transformer, data)
        transformBody(transformer, data)
        return this
    }

    override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstConstructorImpl {
        returnType = returnType.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstConstructorImpl {
        receiverType = receiverType?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstConstructorImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(transformer: AstTransformer<D>, data: D): AstConstructorImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstConstructorImpl {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstConstructorImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDelegatedConstructor(transformer: AstTransformer<D>, data: D): AstConstructorImpl {
        delegatedConstructor = delegatedConstructor?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: AstTransformer<D>, data: D): AstConstructorImpl {
        body = body?.transformSingle(transformer, data)
        return this
    }

    override fun replaceReturnType(newReturnType: AstType) {
        returnType = newReturnType
    }

    override fun replaceReceiverType(newReceiverType: AstType?) {
        receiverType = newReceiverType
    }

    override fun replaceValueParameters(newValueParameters: List<AstValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }
}
