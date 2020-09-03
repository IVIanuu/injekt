package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.PlatformStatus
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
import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
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
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    lateinit var name: Name
    var visibility: Visibility = Visibilities.Public
    var modality: Modality = Modality.FINAL
    var platformStatus: PlatformStatus = PlatformStatus.DEFAULT
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    lateinit var symbol: AstTypeAliasSymbol
    lateinit var expandedType: AstType

    override fun build(): AstTypeAlias {
        return AstTypeAliasImpl(
            annotations,
            origin,
            name,
            visibility,
            modality,
            platformStatus,
            typeParameters,
            symbol,
            expandedType,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeAlias(init: AstTypeAliasBuilder.() -> Unit): AstTypeAlias {
    return AstTypeAliasBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstTypeAlias.copy(init: AstTypeAliasBuilder.() -> Unit = {}): AstTypeAlias {
    val copyBuilder = AstTypeAliasBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.name = name
    copyBuilder.visibility = visibility
    copyBuilder.modality = modality
    copyBuilder.platformStatus = platformStatus
    copyBuilder.typeParameters.addAll(typeParameters)
    copyBuilder.symbol = symbol
    copyBuilder.expandedType = expandedType
    return copyBuilder.apply(init).build()
}
