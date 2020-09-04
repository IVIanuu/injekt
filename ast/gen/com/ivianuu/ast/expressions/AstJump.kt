package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstJump<E : AstTargetElement> : AstPureAbstractElement(), AstExpression {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract val target: AstTarget<E>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitJump(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract fun replaceTarget(newTarget: AstTarget<E>)
}
