package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstMemberDeclaration
import com.ivianuu.ast.declarations.builder.AstNamedDeclarationBuilder
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.visitors.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstMemberDeclarationBuilder : AstNamedDeclarationBuilder {
    abstract override val annotations: MutableList<AstFunctionCall>
    abstract override var origin: AstDeclarationOrigin
    abstract override var attributes: AstDeclarationAttributes
    abstract override var name: Name
    abstract var visibility: Visibility
    abstract var modality: Modality
    abstract var platformStatus: PlatformStatus
    override fun build(): AstMemberDeclaration
}
