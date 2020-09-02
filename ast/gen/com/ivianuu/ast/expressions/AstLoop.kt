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
    abstract override val annotations: List<AstCall>
    abstract val block: AstBlock
    abstract val condition: AstExpression
    abstract val label: AstLabel?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitLoop(this, data)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstLoop

    abstract fun <D> transformBlock(transformer: AstTransformer<D>, data: D): AstLoop

    abstract fun <D> transformCondition(transformer: AstTransformer<D>, data: D): AstLoop

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstLoop
}
