package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
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
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstPropertyBuilder : AstMemberDeclarationBuilder, AstTypeParametersOwnerBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    var receiverType: AstType? = null
    lateinit var returnType: AstType
    override lateinit var name: Name
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
    var hasBackingField: Boolean = false
    var isLocal: Boolean = false
    var isInline: Boolean = false
    var isConst: Boolean = false
    var isLateinit: Boolean = false

    override fun build(): AstProperty {
        return AstPropertyImpl(
            annotations,
            origin,
            receiverType,
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
            hasBackingField,
            isLocal,
            isInline,
            isConst,
            isLateinit,
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
inline fun buildProperty(init: AstPropertyBuilder.() -> Unit): AstProperty {
    return AstPropertyBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstProperty.copy(init: AstPropertyBuilder.() -> Unit = {}): AstProperty {
    val copyBuilder = AstPropertyBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.receiverType = receiverType
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
    copyBuilder.hasBackingField = hasBackingField
    copyBuilder.isLocal = isLocal
    copyBuilder.isInline = isInline
    copyBuilder.isConst = isConst
    copyBuilder.isLateinit = isLateinit
    return copyBuilder.apply(init).build()
}
