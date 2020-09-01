package com.ivianuu.ast.references.impl

import com.ivianuu.ast.references.AstThisReference
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstExplicitThisReference(
    override val labelName: String?,
) : AstThisReference() {
    override var boundSymbol: AbstractAstBasedSymbol<*>? = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstExplicitThisReference {
        return this
    }

    override fun replaceBoundSymbol(newBoundSymbol: AbstractAstBasedSymbol<*>?) {
        boundSymbol = newBoundSymbol
    }
}
