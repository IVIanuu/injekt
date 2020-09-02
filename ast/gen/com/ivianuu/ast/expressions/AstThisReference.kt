package com.ivianuu.ast.expressions

import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstThisReference : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val labelName: String?
    abstract val boundSymbol: AbstractAstSymbol<*>?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitThisReference(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract fun replaceBoundSymbol(newBoundSymbol: AbstractAstSymbol<*>?)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstThisReference
}
