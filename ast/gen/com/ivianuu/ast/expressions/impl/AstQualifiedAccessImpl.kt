package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstQualifiedAccessImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val callee: AstSymbol<*>,
    override val typeArguments: MutableList<AstTypeProjection>,
    override var dispatchReceiver: AstExpression?,
    override var extensionReceiver: AstExpression?,
) : AstQualifiedAccess() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstQualifiedAccessImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        typeArguments.transformInplace(transformer, data)
        return this
    }
}
