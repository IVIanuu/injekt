package com.ivianuu.ast.references.impl

import com.ivianuu.ast.references.AstResolvedCallableReference
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstResolvedCallableReferenceImpl(
    override val name: Name,
    override val resolvedSymbol: AbstractAstBasedSymbol<*>,
    override val inferredTypeArguments: MutableList<ConeKotlinType>,
) : AstResolvedCallableReference() {
    override val candidateSymbol: AbstractAstBasedSymbol<*>? get() = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstResolvedCallableReferenceImpl {
        return this
    }
}
