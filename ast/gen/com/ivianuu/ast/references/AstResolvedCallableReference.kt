package com.ivianuu.ast.references

import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstResolvedCallableReference : AstResolvedNamedReference() {
    abstract override val name: Name
    abstract override val candidateSymbol: AbstractAstSymbol<*>?
    abstract override val resolvedSymbol: AbstractAstSymbol<*>
    abstract val inferredTypeArguments: List<AstType>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitResolvedCallableReference(this, data)
}
