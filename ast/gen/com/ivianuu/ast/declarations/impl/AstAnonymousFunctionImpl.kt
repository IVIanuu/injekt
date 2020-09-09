package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstAnonymousFunctionImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var origin: AstDeclarationOrigin,
    override var attributes: AstDeclarationAttributes,
    override var extensionReceiverType: AstType?,
    override var returnType: AstType,
    override val valueParameters: MutableList<AstValueParameter>,
    override var body: AstBlock?,
    override var type: AstType,
    override var symbol: AstAnonymousFunctionSymbol,
) : AstAnonymousFunction() {
    override val dispatchReceiverType: AstType? get() = null

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        extensionReceiverType?.accept(visitor, data)
        returnType.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstAnonymousFunctionImpl {
        annotations.transformInplace(transformer, data)
        extensionReceiverType = extensionReceiverType?.transformSingle(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        type = type.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceOrigin(newOrigin: AstDeclarationOrigin) {
        origin = newOrigin
    }

    override fun replaceAttributes(newAttributes: AstDeclarationAttributes) {
        attributes = newAttributes
    }

    override fun replaceDispatchReceiverType(newDispatchReceiverType: AstType?) {}

    override fun replaceExtensionReceiverType(newExtensionReceiverType: AstType?) {
        extensionReceiverType = newExtensionReceiverType
    }

    override fun replaceReturnType(newReturnType: AstType) {
        returnType = newReturnType
    }

    override fun replaceValueParameters(newValueParameters: List<AstValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }

    override fun replaceBody(newBody: AstBlock?) {
        body = newBody
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
