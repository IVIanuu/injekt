package com.ivianuu.ast.references

import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstThisReference : AstReference() {
    abstract val labelName: String?
    abstract val boundSymbol: AbstractAstBasedSymbol<*>?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitThisReference(this, data)

    abstract fun replaceBoundSymbol(newBoundSymbol: AbstractAstBasedSymbol<*>?)
}
