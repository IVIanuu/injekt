package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstBlockImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override val statements: MutableList<AstStatement>,
    override var type: AstType,
) : AstBlock() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        statements.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstBlockImpl {
        annotations.transformInplace(transformer, data)
        statements.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceStatements(newStatements: List<AstStatement>) {
        statements.clear()
        statements.addAll(newStatements)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
