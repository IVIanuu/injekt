package com.ivianuu.ast.references

import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstNamedReference : AstReference() {
    abstract val name: Name
    abstract val candidateSymbol: AbstractAstBasedSymbol<*>?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitNamedReference(this, data)
}
