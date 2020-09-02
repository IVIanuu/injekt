package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstTypeAlias
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.builder.AstTypeParametersOwnerBuilder
import com.ivianuu.ast.declarations.impl.AstTypeAliasImpl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstTypeAliasSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeAliasBuilder : AstTypeParametersOwnerBuilder, AstAnnotationContainerBuilder {
    lateinit var origin: AstDeclarationOrigin
    lateinit var status: AstDeclarationStatus
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    lateinit var name: Name
    lateinit var symbol: AstTypeAliasSymbol
    lateinit var expandedType: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()

    override fun build(): AstTypeAlias {
        return AstTypeAliasImpl(
            origin,
            status,
            typeParameters,
            name,
            symbol,
            expandedType,
            annotations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeAlias(init: AstTypeAliasBuilder.() -> Unit): AstTypeAlias {
    return AstTypeAliasBuilder().apply(init).build()
}
