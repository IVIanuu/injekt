package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstTypeAlias
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.impl.AstTypeAliasImpl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstTypeAliasSymbol
import com.ivianuu.ast.types.AstTypeRef
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts

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
    lateinit var expandedTypeRef: AstTypeRef
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()

    override fun build(): AstTypeAlias {
        return AstTypeAliasImpl(
            origin,
            status,
            typeParameters,
            name,
            symbol,
            expandedTypeRef,
            annotations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeAlias(init: AstTypeAliasBuilder.() -> Unit): AstTypeAlias {
    return AstTypeAliasBuilder().apply(init).build()
}
