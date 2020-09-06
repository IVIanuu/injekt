package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.builder.AstMemberDeclarationBuilder
import com.ivianuu.ast.declarations.builder.AstTypeParametersOwnerBuilder
import com.ivianuu.ast.declarations.impl.AstPropertyImpl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
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
class AstPropertyBuilder(override val context: AstContext) : AstMemberDeclarationBuilder, AstTypeParametersOwnerBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    var dispatchReceiverType: AstType? = null
    var extensionReceiverType: AstType? = null
    lateinit var returnType: AstType
    override var name: Name by lazyVar { symbol.callableId.callableName }
    var initializer: AstExpression? = null
    var delegate: AstExpression? = null
    var isVar: Boolean = false
    var getter: AstPropertyAccessor? = null
    var setter: AstPropertyAccessor? = null
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    override var visibility: Visibility = Visibilities.Public
    override var modality: Modality = Modality.FINAL
    override var platformStatus: PlatformStatus = PlatformStatus.DEFAULT
    lateinit var symbol: AstPropertySymbol
    var isLocal: Boolean = false
    var isInline: Boolean = false
    var isConst: Boolean = false
    var isLateinit: Boolean = false
    var isExternal: Boolean = false
    val overriddenProperties: MutableList<AstPropertySymbol> = mutableListOf()

    override fun build(): AstProperty {
        return AstPropertyImpl(
            context,
            annotations,
            origin,
            dispatchReceiverType,
            extensionReceiverType,
            returnType,
            name,
            initializer,
            delegate,
            isVar,
            getter,
            setter,
            typeParameters,
            visibility,
            modality,
            platformStatus,
            symbol,
            isLocal,
            isInline,
            isConst,
            isLateinit,
            isExternal,
            overriddenProperties,
        )
    }


    @Deprecated("Modification of 'attributes' has no impact for AstPropertyBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildProperty(init: AstPropertyBuilder.() -> Unit): AstProperty {
    return AstPropertyBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstProperty.copy(init: AstPropertyBuilder.() -> Unit = {}): AstProperty {
    val copyBuilder = AstPropertyBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.dispatchReceiverType = dispatchReceiverType
    copyBuilder.extensionReceiverType = extensionReceiverType
    copyBuilder.returnType = returnType
    copyBuilder.name = name
    copyBuilder.initializer = initializer
    copyBuilder.delegate = delegate
    copyBuilder.isVar = isVar
    copyBuilder.getter = getter
    copyBuilder.setter = setter
    copyBuilder.typeParameters.addAll(typeParameters)
    copyBuilder.visibility = visibility
    copyBuilder.modality = modality
    copyBuilder.platformStatus = platformStatus
    copyBuilder.symbol = symbol
    copyBuilder.isLocal = isLocal
    copyBuilder.isInline = isInline
    copyBuilder.isConst = isConst
    copyBuilder.isLateinit = isLateinit
    copyBuilder.isExternal = isExternal
    copyBuilder.overriddenProperties.addAll(overriddenProperties)
    return copyBuilder.apply(init).build()
}
