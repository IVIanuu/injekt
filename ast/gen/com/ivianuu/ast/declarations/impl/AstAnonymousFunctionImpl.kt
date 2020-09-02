package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstAnonymousFunctionImpl(
    override val origin: AstDeclarationOrigin,
    override var receiverType: AstType?,
    override var returnType: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val valueParameters: MutableList<AstValueParameter>,
    override var body: AstBlock?,
    override var type: AstType,
    override val symbol: AstAnonymousFunctionSymbol,
    override val label: String?,
) : AstAnonymousFunction() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        receiverType?.accept(visitor, data)
        returnType.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstAnonymousFunctionImpl {
        receiverType = receiverType?.transformSingle(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        valueParameters.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        type = type.transformSingle(transformer, data)
        return this
    }
}
