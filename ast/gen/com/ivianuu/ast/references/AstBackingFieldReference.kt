package com.ivianuu.ast.references

import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.symbols.impl.AstBackingFieldSymbol
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstBackingFieldReference : AstResolvedNamedReference() {
    abstract override val name: Name
    abstract override val candidateSymbol: AbstractAstBasedSymbol<*>?
    abstract override val resolvedSymbol: AstBackingFieldSymbol

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitBackingFieldReference(this, data)
}
