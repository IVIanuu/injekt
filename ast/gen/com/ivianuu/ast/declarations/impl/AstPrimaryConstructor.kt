package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstPrimaryConstructor(
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override var receiverType: AstType?,
    override val valueParameters: MutableList<AstValueParameter>,
    override var status: AstDeclarationStatus,
    override val annotations: MutableList<AstFunctionCall>,
    override val symbol: AstConstructorSymbol,
    override var delegatedConstructor: AstDelegatedConstructorCall?,
    override var body: AstBlock?,
) : AstConstructor() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val isPrimary: Boolean get() = true

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnType.accept(visitor, data)
        receiverType?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        delegatedConstructor?.accept(visitor, data)
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        transformReturnType(transformer, data)
        transformReceiverType(transformer, data)
        transformValueParameters(transformer, data)
        transformStatus(transformer, data)
        transformAnnotations(transformer, data)
        transformDelegatedConstructor(transformer, data)
        transformBody(transformer, data)
        return this
    }

    override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        returnType = returnType.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstPrimaryConstructor {
        receiverType = receiverType?.transformSingle(transformer, data)
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
