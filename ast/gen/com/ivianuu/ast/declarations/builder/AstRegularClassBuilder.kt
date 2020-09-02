package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.builder.AstClassBuilder
import com.ivianuu.ast.declarations.builder.AstTypeParametersOwnerBuilder
import com.ivianuu.ast.declarations.impl.AstRegularClassImpl
import com.ivianuu.ast.expressions.AstFunctionCall
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
open class AstRegularClassBuilder : AstClassBuilder, AstTypeParametersOwnerBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    override var classKind: ClassKind = ClassKind.CLASS
    override val declarations: MutableList<AstDeclaration> = mutableListOf()
    open lateinit var name: Name
    open var visibility: Visibility = Visibilities.Public
    open var isExpect: Boolean = false
    open var isActual: Boolean = false
    open var modality: Modality = Modality.FINAL
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
            typeParameters,
            classKind,
            declarations,
            name,
            visibility,
            isExpect,
            isActual,
            modality,
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
