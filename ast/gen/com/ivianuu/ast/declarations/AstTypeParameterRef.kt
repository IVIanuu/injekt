package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstTypeParameterRef : AstElement {
    val symbol: AstTypeParameterSymbol

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTypeParameterRef(this, data)
}
