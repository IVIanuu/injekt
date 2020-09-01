package com.ivianuu.ast.references.impl

import com.ivianuu.ast.references.AstThisReference
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstImplicitThisReference(
    override val boundSymbol: AbstractAstBasedSymbol<*>?,
) : AstThisReference() {
    override val labelName: String? get() = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstImplicitThisReference {
        return this
    }

    override fun replaceBoundSymbol(newBoundSymbol: AbstractAstBasedSymbol<*>?) {}
}
