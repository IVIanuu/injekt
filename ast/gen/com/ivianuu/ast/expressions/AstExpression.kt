package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstVarargElement
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstExpression : AstStatement, AstVarargElement {
    val type: AstType
    override val annotations: List<AstFunctionCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitExpression(this, data)
}
