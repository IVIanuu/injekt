package com.ivianuu.ast.expressions

import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstCall : AstStatement {
    override val annotations: List<AstFunctionCall>
    val valueArguments: List<AstExpression>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCall(this, data)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstCall
}
