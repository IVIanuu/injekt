package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstNamedDeclaration
import com.ivianuu.ast.declarations.builder.AstDeclarationBuilder
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstNamedDeclarationBuilder : AstDeclarationBuilder {
    abstract override val annotations: MutableList<AstFunctionCall>
    abstract override var origin: AstDeclarationOrigin
    abstract override var attributes: AstDeclarationAttributes
    abstract var name: Name
    override fun build(): AstNamedDeclaration
}
