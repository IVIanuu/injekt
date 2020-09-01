package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstAnonymousInitializer
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.impl.AstAnonymousInitializerImpl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.symbols.impl.AstAnonymousInitializerSymbol
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAnonymousInitializerBuilder {
    lateinit var origin: AstDeclarationOrigin
    var body: AstBlock? = null
    lateinit var symbol: AstAnonymousInitializerSymbol

    fun build(): AstAnonymousInitializer {
        return AstAnonymousInitializerImpl(
            origin,
            body,
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousInitializer(init: AstAnonymousInitializerBuilder.() -> Unit): AstAnonymousInitializer {
    return AstAnonymousInitializerBuilder().apply(init).build()
}
