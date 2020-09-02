package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstReturnImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override var result: AstExpression,
    override val target: AstFunctionSymbol<*>,
) : AstReturn() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstReturnImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        result = result.transformSingle(transformer, data)
        return this
    }
}
