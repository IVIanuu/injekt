package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstConstructorImpl(
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override var receiverType: AstType?,
    override val valueParameters: MutableList<AstValueParameter>,
    override val annotations: MutableList<AstFunctionCall>,
    override val symbol: AstConstructorSymbol,
    override var delegatedConstructor: AstDelegatedConstructorCall?,
    override var body: AstBlock?,
) : AstConstructor() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val isPrimary: Boolean get() = false

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnType.accept(visitor, data)
        receiverType?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
        delegatedConstructor?.accept(visitor, data)
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstConstructorImpl {
        returnType = returnType.transformSingle(transformer, data)
        receiverType = receiverType?.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)
        annotations.transformInplace(transformer, data)
        delegatedConstructor = delegatedConstructor?.transformSingle(transformer, data)
        body = body?.transformSingle(transformer, data)
        return this
    }
}
