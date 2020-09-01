package com.ivianuu.ast.references

import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.symbols.impl.AstBackingFieldSymbol
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstBackingFieldReference : AstResolvedNamedReference() {
    abstract override val name: Name
    abstract override val candidateSymbol: AbstractAstSymbol<*>?
    abstract override val resolvedSymbol: AstBackingFieldSymbol

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitBackingFieldReference(this, data)
}
