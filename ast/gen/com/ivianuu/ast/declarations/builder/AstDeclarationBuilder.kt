package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstDeclarationBuilder : AstAnnotationContainerBuilder {
    abstract override val annotations: MutableList<AstFunctionCall>
    abstract var origin: AstDeclarationOrigin
    abstract var attributes: AstDeclarationAttributes
    override fun build(): AstDeclaration
}
