package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeAlias
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.builder.AstTypeParametersOwnerBuilder
import com.ivianuu.ast.declarations.impl.AstTypeAliasImpl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstTypeAliasSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeAliasBuilder : AstTypeParametersOwnerBuilder {
    var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    lateinit var name: Name
    var visibility: Visibility = Visibilities.Public
    var isExpect: Boolean = false
    var isActual: Boolean = false
    var modality: Modality = Modality.FINAL
    lateinit var symbol: AstTypeAliasSymbol
    lateinit var expandedType: AstType
    val annotations: MutableList<AstFunctionCall> = mutableListOf()

    override fun build(): AstTypeAlias {
        return AstTypeAliasImpl(
            origin,
            typeParameters,
            name,
            visibility,
            isExpect,
            isActual,
            modality,
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
