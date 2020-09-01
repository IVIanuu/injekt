package com.ivianuu.ast.references.impl

import com.ivianuu.ast.references.AstResolvedNamedReference
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstResolvedNamedReferenceImpl(
    override val name: Name,
    override val resolvedSymbol: AbstractAstBasedSymbol<*>,
) : AstResolvedNamedReference() {
    override val candidateSymbol: AbstractAstBasedSymbol<*>? get() = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstResolvedNamedReferenceImpl {
        return this
    }
}
