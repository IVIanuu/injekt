package com.ivianuu.ast.references

import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstResolvedNamedReference : AstNamedReference() {
    abstract override val name: Name
    abstract override val candidateSymbol: AbstractAstBasedSymbol<*>?
    abstract val resolvedSymbol: AbstractAstBasedSymbol<*>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitResolvedNamedReference(this, data)
}
