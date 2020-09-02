package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstStringConcatenationCall : AstCall, AstExpression() {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val valueArguments: List<AstExpression>
    abstract override val type: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitStringConcatenationCall(this, data)
}
