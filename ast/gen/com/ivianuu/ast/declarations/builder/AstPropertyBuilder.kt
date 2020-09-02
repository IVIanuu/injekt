package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstField
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.builder.AstTypeParametersOwnerBuilder
import com.ivianuu.ast.declarations.impl.AstPropertyImpl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstBackingFieldSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
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
class AstPropertyBuilder : AstTypeParametersOwnerBuilder {
    var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    lateinit var returnType: AstType
    var receiverType: AstType? = null
    lateinit var name: Name
    var initializer: AstExpression? = null
    var delegate: AstExpression? = null
    var isVar: Boolean = false
    var getter: AstPropertyAccessor? = null
    var setter: AstPropertyAccessor? = null
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    lateinit var symbol: AstPropertySymbol
    var isLocal: Boolean = false
    var visibility: Visibility = Visibilities.Public
    var isExpect: Boolean = false
    var isActual: Boolean = false
    var modality: Modality = Modality.FINAL
    var isInline: Boolean = false
    var isConst: Boolean = false
    var isLateinit: Boolean = false

    override fun build(): AstProperty {
        return AstPropertyImpl(
            origin,
            returnType,
            receiverType,
            name,
            initializer,
            delegate,
            isVar,
            getter,
            setter,
            annotations,
            typeParameters,
            symbol,
            isLocal,
            visibility,
            isExpect,
            isActual,
            modality,
            isInline,
            isConst,
            isLateinit,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildProperty(init: AstPropertyBuilder.() -> Unit): AstProperty {
    return AstPropertyBuilder().apply(init).build()
}
