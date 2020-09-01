package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.declarations.impl.AstOuterClassTypeParameterRef
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstOuterClassTypeParameterRefBuilder {
    lateinit var symbol: AstTypeParameterSymbol

    fun build(): AstTypeParameterRef {
        return AstOuterClassTypeParameterRef(
            symbol,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildOuterClassTypeParameterRef(init: AstOuterClassTypeParameterRefBuilder.() -> Unit): AstTypeParameterRef {
    return AstOuterClassTypeParameterRefBuilder().apply(init).build()
}
