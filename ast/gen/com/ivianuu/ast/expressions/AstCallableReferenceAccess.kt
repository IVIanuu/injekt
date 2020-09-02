package com.ivianuu.ast.expressions

import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstCallableReferenceAccess : AstQualifiedAccess() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract override val typeArguments: List<AstTypeProjection>
    abstract override val dispatchReceiver: AstExpression?
    abstract override val extensionReceiver: AstExpression?
    abstract val callee: AstCallableSymbol<*>
    abstract val hasQuestionMarkAtLHS: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCallableReferenceAccess(this, data)
}
