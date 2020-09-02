package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAnonymousFunction : AstFunction<AstAnonymousFunction>, AstExpression() {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val receiverType: AstType?
    abstract override val returnType: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val body: AstBlock?
    abstract override val type: AstType
    abstract override val symbol: AstAnonymousFunctionSymbol
    abstract val label: String?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitAnonymousFunction(this, data)
}
