package com.ivianuu.ast.references

import com.ivianuu.ast.symbols.AbstractAstSymbol
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstResolvedNamedReference : AstNamedReference() {
    abstract override val name: Name
    abstract override val candidateSymbol: AbstractAstSymbol<*>?
    abstract val resolvedSymbol: AbstractAstSymbol<*>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitResolvedNamedReference(this, data)
}
