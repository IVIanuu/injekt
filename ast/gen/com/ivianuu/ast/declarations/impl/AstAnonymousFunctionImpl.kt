package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstAnonymousFunctionImpl(
    override val origin: AstDeclarationOrigin,
    override val annotations: MutableList<AstCall>,
    override var returnType: AstType,
    override var receiverType: AstType?,
    override val valueParameters: MutableList<AstValueParameter>,
    override var body: AstBlock?,
    override var type: AstType,
    override val symbol: AstAnonymousFunctionSymbol,
    override var label: AstLabel?,
    override val isLambda: Boolean,
    override val typeParameters: MutableList<AstTypeParameter>,
) : AstAnonymousFunction() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        returnType.accept(visitor, data)
        receiverType?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        type.accept(visitor, data)
        label?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstAnonymousFunctionImpl {
        transformAnnotations(transformer, data)
        transformReturnType(transformer, data)
        transformReceiverType(transformer, data)
        transformValueParameters(transformer, data)
        transformBody(transformer, data)
        type = type.transformSingle(transformer, data)
        label = label?.transformSingle(transformer, data)
        transformTypeParameters(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstAnonymousFunctionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstAnonymousFunctionImpl {
        returnType = returnType.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstAnonymousFunctionImpl {
        receiverType = receiverType?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(transformer: AstTransformer<D>, data: D): AstAnonymousFunctionImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: AstTransformer<D>, data: D): AstAnonymousFunctionImpl {
        body = body?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstAnonymousFunctionImpl {
        typeParameters.transformInplace(transformer, data)
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

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
