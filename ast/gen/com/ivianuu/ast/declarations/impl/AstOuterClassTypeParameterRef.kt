package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstOuterClassTypeParameterRef(
    override val symbol: AstTypeParameterSymbol,
) : AstPureAbstractElement(), AstTypeParameterRef {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstOuterClassTypeParameterRef {
        return this
    }
}
