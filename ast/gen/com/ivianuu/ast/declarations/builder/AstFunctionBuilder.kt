package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.types.AstTypeRef

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstFunctionBuilder : AstAnnotationContainerBuilder {
    abstract override val annotations: MutableList<AstAnnotationCall>
    abstract var origin: AstDeclarationOrigin
    abstract var attributes: AstDeclarationAttributes
    abstract var returnTypeRef: AstTypeRef
    abstract val valueParameters: MutableList<AstValueParameter>
    abstract var body: AstBlock?
    override fun build(): AstFunction<*>
}
