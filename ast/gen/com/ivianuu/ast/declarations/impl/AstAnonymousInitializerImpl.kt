package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.declarations.AstAnonymousInitializer
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstAnonymousInitializerSymbol
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstAnonymousInitializerImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var origin: AstDeclarationOrigin,
    override var attributes: AstDeclarationAttributes,
    override var body: AstBlock,
    override var symbol: AstAnonymousInitializerSymbol,
) : AstAnonymousInitializer() {
    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstAnonymousInitializerImpl {
        annotations.transformInplace(transformer, data)
        body = body.transformSingle(transformer, data)
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

    override fun replaceBody(newBody: AstBlock) {
        body = newBody
    }
}
