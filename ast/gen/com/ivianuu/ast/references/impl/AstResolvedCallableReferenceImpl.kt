package com.ivianuu.ast.references.impl

import com.ivianuu.ast.references.AstResolvedCallableReference
import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstResolvedCallableReferenceImpl(
    override val name: Name,
    override val resolvedSymbol: AbstractAstSymbol<*>,
    override val inferredTypeArguments: MutableList<AstType>,
) : AstResolvedCallableReference() {
    override val candidateSymbol: AbstractAstSymbol<*>? get() = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        inferredTypeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstResolvedCallableReferenceImpl {
        inferredTypeArguments.transformInplace(transformer, data)
        return this
    }
}
