package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstAnonymousInitializer
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.impl.AstAnonymousInitializerImpl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstAnonymousInitializerSymbol
import com.ivianuu.ast.utils.lazyVar
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAnonymousInitializerBuilder(override val context: AstContext) : AstBuilder {
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    var body: AstBlock? = null
    var symbol: AstAnonymousInitializerSymbol by lazyVar { AstAnonymousInitializerSymbol() }

    fun build(): AstAnonymousInitializer {
        return AstAnonymousInitializerImpl(
            context,
            annotations,
            origin,
            body,
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildAnonymousInitializer(init: AstAnonymousInitializerBuilder.() -> Unit = {}): AstAnonymousInitializer {
    return AstAnonymousInitializerBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstAnonymousInitializer.copy(init: AstAnonymousInitializerBuilder.() -> Unit = {}): AstAnonymousInitializer {
    val copyBuilder = AstAnonymousInitializerBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.body = body
    copyBuilder.symbol = symbol
    return copyBuilder.apply(init).build()
}
