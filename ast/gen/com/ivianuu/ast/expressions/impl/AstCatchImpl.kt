package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstCatchImpl(
    override var parameter: AstValueParameter,
    override var block: AstBlock,
) : AstCatch() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        parameter.accept(visitor, data)
        block.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstCatchImpl {
        transformOtherChildren(transformer, data)
        return this
    }
}
