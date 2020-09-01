package com.ivianuu.ast.references.impl

import com.ivianuu.ast.references.AstDelegateFieldReference
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.symbols.impl.AstDelegateFieldSymbol
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstDelegateFieldReferenceImpl(
    override val candidateSymbol: AbstractAstBasedSymbol<*>?,
    override val resolvedSymbol: AstDelegateFieldSymbol<*>,
) : AstDelegateFieldReference() {
    override val name: Name get() = Name.identifier("\$delegate")

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstDelegateFieldReferenceImpl {
        return this
    }
}
