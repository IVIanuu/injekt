package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.AstFunctionBuilder
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

@AstBuilderDsl
interface AstAbstractConstructorBuilder : AstFunctionBuilder {
    abstract override val annotations: MutableList<AstFunctionCall>
    abstract override var origin: AstDeclarationOrigin
    abstract override var attributes: AstDeclarationAttributes
    abstract override var returnType: AstType
    abstract override val valueParameters: MutableList<AstValueParameter>
    abstract override var body: AstBlock?
    abstract var dispatchReceiverType: AstType?
    abstract var extensionReceiverType: AstType?
    abstract var symbol: AstConstructorSymbol
    abstract var delegatedConstructor: AstDelegatedConstructorCall?
    abstract var visibility: Visibility
    override fun build(): AstConstructor
}
