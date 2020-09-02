package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstLoop : AstPureAbstractElement(), AstStatement, AstTargetElement {
    abstract override val annotations: List<AstFunctionCall>
    abstract val block: AstBlock
    abstract val condition: AstExpression
    abstract val label: AstLabel?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitLoop(this, data)

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstLoop
}
