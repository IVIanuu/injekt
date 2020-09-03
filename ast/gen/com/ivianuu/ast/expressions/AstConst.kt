package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstConst<T> : AstPureAbstractElement(), AstExpression {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract val kind: AstConstKind<T>
    abstract val value: T

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitConst(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract fun replaceKind(newKind: AstConstKind<T>)

    abstract fun replaceValue(newValue: T)
}
