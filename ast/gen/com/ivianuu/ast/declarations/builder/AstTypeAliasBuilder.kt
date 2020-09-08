package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilder
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
import com.ivianuu.ast.utils.lazyVar
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeAliasBuilder(override val context: AstContext) : AstTypeParametersOwnerBuilder {
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    var attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    var name: Name by lazyVar { symbol.fqName.shortName() }
    var visibility: Visibility = Visibilities.Public
    var modality: Modality = Modality.FINAL
    var platformStatus: PlatformStatus = PlatformStatus.DEFAULT
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    lateinit var symbol: AstTypeAliasSymbol
    lateinit var expandedType: AstType

    override fun build(): AstTypeAlias {
        return AstTypeAliasImpl(
            context,
            annotations,
            origin,
            attributes,
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
inline fun AstBuilder.buildTypeAlias(init: AstTypeAliasBuilder.() -> Unit): AstTypeAlias {
    return AstTypeAliasBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstTypeAlias.copy(init: AstTypeAliasBuilder.() -> Unit = {}): AstTypeAlias {
    val copyBuilder = AstTypeAliasBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.attributes = attributes
    copyBuilder.name = name
    copyBuilder.visibility = visibility
    copyBuilder.modality = modality
    copyBuilder.platformStatus = platformStatus
    copyBuilder.typeParameters.addAll(typeParameters)
    copyBuilder.symbol = symbol
    copyBuilder.expandedType = expandedType
    return copyBuilder.apply(init).build()
}
