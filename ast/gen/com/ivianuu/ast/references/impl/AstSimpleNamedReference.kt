package com.ivianuu.ast.references.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.references.AstNamedReference
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class AstSimpleNamedReference @AstImplementationDetail constructor(
    override val name: Name,
    override val candidateSymbol: AbstractAstBasedSymbol<*>?,
) : AstNamedReference() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstSimpleNamedReference {
        return this
    }
}
