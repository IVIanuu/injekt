package com.ivianuu.ast.expressions

import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstQualifiedAccess : AstExpression {
    override val type: AstType
    override val annotations: List<AstFunctionCall>
    val callee: AstSymbol<*>
    val typeArguments: List<AstTypeProjection>
    val dispatchReceiver: AstExpression?
    val extensionReceiver: AstExpression?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitQualifiedAccess(this, data)
}
