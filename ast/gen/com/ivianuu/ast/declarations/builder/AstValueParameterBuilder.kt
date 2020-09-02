package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.impl.AstValueParameterImpl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstValueParameterBuilder {
    open lateinit var origin: AstDeclarationOrigin
    open lateinit var returnType: AstType
    open lateinit var name: Name
    open val annotations: MutableList<AstFunctionCall> = mutableListOf()
    open lateinit var symbol: AstValueParameterSymbol
    open var defaultValue: AstExpression? = null
    open var isCrossinline: Boolean = false
    open var isNoinline: Boolean = false
    open var isVararg: Boolean = false

    @OptIn(AstImplementationDetail::class)
    fun build(): AstValueParameter {
        return AstValueParameterImpl(
            origin,
            returnType,
            name,
            annotations,
            symbol,
            defaultValue,
            isCrossinline,
            isNoinline,
            isVararg,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildValueParameter(init: AstValueParameterBuilder.() -> Unit): AstValueParameter {
    return AstValueParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildValueParameterCopy(original: AstValueParameter, init: AstValueParameterBuilder.() -> Unit): AstValueParameter {
    val copyBuilder = AstValueParameterBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.returnType = original.returnType
    copyBuilder.name = original.name
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.symbol = original.symbol
    copyBuilder.defaultValue = original.defaultValue
    copyBuilder.isCrossinline = original.isCrossinline
    copyBuilder.isNoinline = original.isNoinline
    copyBuilder.isVararg = original.isVararg
    return copyBuilder.apply(init).build()
}
