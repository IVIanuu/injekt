package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstReturnExpression : AstJump<AstFunction<*>>() {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val target: AstTarget<AstFunction<*>>
    abstract val result: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitReturnExpression(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstReturnExpression

    abstract fun <D> transformResult(transformer: AstTransformer<D>, data: D): AstReturnExpression

    abstract fun <D> transformOtherChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstReturnExpression
}
