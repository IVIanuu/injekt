package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.impl.AstDefaultSetterValueParameter
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
class AstDefaultSetterValueParameterBuilder {
    lateinit var origin: AstDeclarationOrigin
    lateinit var returnType: AstType
    var receiverType: AstType? = null
    var initializer: AstExpression? = null
    var delegate: AstExpression? = null
    var isVar: Boolean = false
    var isVal: Boolean = true
    var getter: AstPropertyAccessor? = null
    var setter: AstPropertyAccessor? = null
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var symbol: AstValueParameterSymbol
    var defaultValue: AstExpression? = null
    var isCrossinline: Boolean = false
    var isNoinline: Boolean = false
    var isVararg: Boolean = false

    fun build(): AstValueParameter {
        return AstDefaultSetterValueParameter(
            origin,
            returnType,
            receiverType,
            initializer,
            delegate,
            isVar,
            isVal,
            getter,
            setter,
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
inline fun buildDefaultSetterValueParameter(init: AstDefaultSetterValueParameterBuilder.() -> Unit): AstValueParameter {
    return AstDefaultSetterValueParameterBuilder().apply(init).build()
}
