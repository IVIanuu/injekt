package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.AstNamedDeclarationBuilder
import com.ivianuu.ast.declarations.impl.AstValueParameterImpl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.utils.lazyVar
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstValueParameterBuilder(override val context: AstContext) : AstNamedDeclarationBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    open lateinit var returnType: AstType
    override var name: Name by lazyVar { symbol.callableId.callableName }
    open lateinit var symbol: AstValueParameterSymbol
    open var defaultValue: AstExpression? = null
    open var isCrossinline: Boolean = false
    open var isNoinline: Boolean = false
    open var isVararg: Boolean = false
    open var correspondingProperty: AstPropertySymbol? = null

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstValueParameter {
        return AstValueParameterImpl(
            context,
            annotations,
            origin,
            returnType,
            name,
            symbol,
            defaultValue,
            isCrossinline,
            isNoinline,
            isVararg,
            correspondingProperty,
        )
    }


    @Deprecated("Modification of 'attributes' has no impact for AstValueParameterBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildValueParameter(init: AstValueParameterBuilder.() -> Unit): AstValueParameter {
    return AstValueParameterBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstValueParameter.copy(init: AstValueParameterBuilder.() -> Unit = {}): AstValueParameter {
    val copyBuilder = AstValueParameterBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.returnType = returnType
    copyBuilder.name = name
    copyBuilder.symbol = symbol
    copyBuilder.defaultValue = defaultValue
    copyBuilder.isCrossinline = isCrossinline
    copyBuilder.isNoinline = isNoinline
    copyBuilder.isVararg = isVararg
    copyBuilder.correspondingProperty = correspondingProperty
    return copyBuilder.apply(init).build()
}
