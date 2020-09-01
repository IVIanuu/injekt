package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.declarations.impl.AstConstructedClassTypeParameterRef
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstConstructedClassTypeParameterRefBuilder {
    lateinit var symbol: AstTypeParameterSymbol

    fun build(): AstTypeParameterRef {
        return AstConstructedClassTypeParameterRef(
            symbol,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildConstructedClassTypeParameterRef(init: AstConstructedClassTypeParameterRefBuilder.() -> Unit): AstTypeParameterRef {
    return AstConstructedClassTypeParameterRefBuilder().apply(init).build()
}
