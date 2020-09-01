package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.types.AstTypeRef

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstAbstractConstructorBuilder : AstFunctionBuilder {
    abstract override val annotations: MutableList<AstAnnotationCall>
    abstract override var origin: AstDeclarationOrigin
    abstract override var attributes: AstDeclarationAttributes
    abstract override var returnTypeRef: AstTypeRef
    abstract override val valueParameters: MutableList<AstValueParameter>
    abstract override var body: AstBlock?
    abstract var receiverTypeRef: AstTypeRef?
    abstract val typeParameters: MutableList<AstTypeParameterRef>
    abstract var status: AstDeclarationStatus
    abstract var symbol: AstConstructorSymbol
    abstract var delegatedConstructor: AstDelegatedConstructorCall?
    override fun build(): AstConstructor
}
