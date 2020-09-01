package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitUnitType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class AstUnitExpression @AstImplementationDetail constructor(
    override val annotations: MutableList<AstAnnotationCall>,
) : AstExpression() {
    override var type: AstType = AstImplicitUnitType()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstUnitExpression {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstUnitExpression {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
