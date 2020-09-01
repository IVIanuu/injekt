package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstContinueExpression
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitNothingType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstContinueExpressionImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override val target: AstTarget<AstLoop>,
) : AstContinueExpression() {
    override var type: AstType = AstImplicitNothingType()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstContinueExpressionImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstContinueExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
