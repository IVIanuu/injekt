package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstNamedArgumentExpression
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstNamedArgumentExpressionImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var expression: AstExpression,
    override val isSpread: Boolean,
    override val name: Name,
) : AstNamedArgumentExpression() {
    override val type: AstType get() = expression.type

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        expression.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstNamedArgumentExpressionImpl {
        transformAnnotations(transformer, data)
        expression = expression.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstNamedArgumentExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {}
}
