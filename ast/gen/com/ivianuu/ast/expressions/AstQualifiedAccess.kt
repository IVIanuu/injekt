package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstQualifiedAccess : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val typeArguments: List<AstTypeProjection>
    abstract val dispatchReceiver: AstExpression?
    abstract val extensionReceiver: AstExpression?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitQualifiedAccess(this, data)
}
