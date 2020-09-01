package com.ivianuu.ast.references

import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstResolvedCallableReference : AstResolvedNamedReference() {
    abstract override val name: Name
    abstract override val candidateSymbol: AbstractAstBasedSymbol<*>?
    abstract override val resolvedSymbol: AbstractAstBasedSymbol<*>
    abstract val inferredTypeArguments: List<ConeKotlinType>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitResolvedCallableReference(this, data)
}
