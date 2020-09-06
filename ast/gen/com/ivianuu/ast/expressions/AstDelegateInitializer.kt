package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstDelegateInitializer : AstPureAbstractElement(), AstElement {
    abstract override val context: AstContext
    abstract val delegatedSuperType: AstType
    abstract val expression: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDelegateInitializer(this, data)

    abstract fun replaceDelegatedSuperType(newDelegatedSuperType: AstType)

    abstract fun replaceExpression(newExpression: AstExpression)
}
