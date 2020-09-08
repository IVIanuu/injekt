package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.builder.AstClassBuilder
import com.ivianuu.ast.declarations.builder.AstDeclarationContainerBuilder
import com.ivianuu.ast.declarations.builder.AstTypeParametersOwnerBuilder
import com.ivianuu.ast.declarations.impl.AstRegularClassImpl
import com.ivianuu.ast.expressions.AstDelegateInitializer
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.utils.lazyVar
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstRegularClassBuilder(override val context: AstContext) : AstClassBuilder, AstTypeParametersOwnerBuilder, AstDeclarationContainerBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    override var attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    open var name: Name by lazyVar { symbol.fqName.shortName() }
    open var visibility: Visibility = Visibilities.Public
    open var modality: Modality = Modality.FINAL
    open var platformStatus: PlatformStatus = PlatformStatus.DEFAULT
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    override val declarations: MutableList<AstDeclaration> = mutableListOf()
    override var classKind: ClassKind = ClassKind.CLASS
    override val delegateInitializers: MutableList<AstDelegateInitializer> = mutableListOf()
    open lateinit var symbol: AstRegularClassSymbol
    override val superTypes: MutableList<AstType> = mutableListOf()
    open var isInline: Boolean = false
    open var isCompanion: Boolean = false
    open var isFun: Boolean = false
    open var isData: Boolean = false
    open var isInner: Boolean = false
    open var isExternal: Boolean = false

    override fun build(): AstRegularClass {
        return AstRegularClassImpl(
            context,
            annotations,
            origin,
            attributes,
            name,
            visibility,
            modality,
            platformStatus,
            typeParameters,
            declarations,
            classKind,
            delegateInitializers,
            symbol,
            superTypes,
            isInline,
            isCompanion,
            isFun,
            isData,
            isInner,
            isExternal,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildRegularClass(init: AstRegularClassBuilder.() -> Unit): AstRegularClass {
    return AstRegularClassBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstRegularClass.copy(init: AstRegularClassBuilder.() -> Unit = {}): AstRegularClass {
    val copyBuilder = AstRegularClassBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.attributes = attributes
    copyBuilder.name = name
    copyBuilder.visibility = visibility
    copyBuilder.modality = modality
    copyBuilder.platformStatus = platformStatus
    copyBuilder.typeParameters.addAll(typeParameters)
    copyBuilder.declarations.addAll(declarations)
    copyBuilder.classKind = classKind
    copyBuilder.delegateInitializers.addAll(delegateInitializers)
    copyBuilder.symbol = symbol
    copyBuilder.superTypes.addAll(superTypes)
    copyBuilder.isInline = isInline
    copyBuilder.isCompanion = isCompanion
    copyBuilder.isFun = isFun
    copyBuilder.isData = isData
    copyBuilder.isInner = isInner
    copyBuilder.isExternal = isExternal
    return copyBuilder.apply(init).build()
}
