package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.AstNamedDeclarationBuilder
import com.ivianuu.ast.declarations.impl.AstValueParameterImpl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstValueParameterBuilder : AstNamedDeclarationBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    open lateinit var returnType: AstType
    override lateinit var name: Name
    open lateinit var symbol: AstValueParameterSymbol
    open var defaultValue: AstExpression? = null
    open var isCrossinline: Boolean = false
    open var isNoinline: Boolean = false
    open var isVararg: Boolean = false

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstValueParameter {
        return AstValueParameterImpl(
            annotations,
            origin,
            returnType,
            name,
            symbol,
            defaultValue,
            isCrossinline,
            isNoinline,
            isVararg,
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
inline fun buildValueParameter(init: AstValueParameterBuilder.() -> Unit): AstValueParameter {
    return AstValueParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstValueParameter.copy(init: AstValueParameterBuilder.() -> Unit = {}): AstValueParameter {
    val copyBuilder = AstValueParameterBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.returnType = returnType
    copyBuilder.name = name
    copyBuilder.symbol = symbol
    copyBuilder.defaultValue = defaultValue
    copyBuilder.isCrossinline = isCrossinline
    copyBuilder.isNoinline = isNoinline
    copyBuilder.isVararg = isVararg
    return copyBuilder.apply(init).build()
}
