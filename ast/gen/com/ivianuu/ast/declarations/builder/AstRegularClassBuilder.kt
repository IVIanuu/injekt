package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
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
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.AstType
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
open class AstRegularClassBuilder : AstClassBuilder, AstTypeParametersOwnerBuilder, AstDeclarationContainerBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    open lateinit var name: Name
    open var visibility: Visibility = Visibilities.Public
    open var modality: Modality = Modality.FINAL
    open var platformStatus: PlatformStatus = PlatformStatus.DEFAULT
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    override val declarations: MutableList<AstDeclaration> = mutableListOf()
    override var classKind: ClassKind = ClassKind.CLASS
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
            annotations,
            origin,
            name,
            visibility,
            modality,
            platformStatus,
            typeParameters,
            declarations,
            classKind,
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

    @Deprecated("Modification of 'attributes' has no impact for AstRegularClassBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildRegularClass(init: AstRegularClassBuilder.() -> Unit): AstRegularClass {
    return AstRegularClassBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstRegularClass.copy(init: AstRegularClassBuilder.() -> Unit = {}): AstRegularClass {
    val copyBuilder = AstRegularClassBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.name = name
    copyBuilder.visibility = visibility
    copyBuilder.modality = modality
    copyBuilder.platformStatus = platformStatus
    copyBuilder.typeParameters.addAll(typeParameters)
    copyBuilder.declarations.addAll(declarations)
    copyBuilder.classKind = classKind
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
