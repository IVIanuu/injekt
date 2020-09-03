package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstVarargElement
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstExpression : AstStatement, AstVarargElement {
    override val annotations: List<AstFunctionCall>
    val type: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitExpression(this, data)

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    fun replaceType(newType: AstType)
}
