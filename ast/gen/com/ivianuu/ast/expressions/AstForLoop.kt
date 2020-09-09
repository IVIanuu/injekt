package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstForLoop : AstLoop() {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract override val body: AstExpression
    abstract val loopRange: AstExpression
    abstract val loopParameter: AstProperty

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitForLoop(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract override fun replaceBody(newBody: AstExpression)

    abstract fun replaceLoopRange(newLoopRange: AstExpression)

    abstract fun replaceLoopParameter(newLoopParameter: AstProperty)
}
