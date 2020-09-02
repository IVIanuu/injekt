package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.impl.AstEnumEntryImpl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
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
class AstEnumEntryBuilder {
    var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    lateinit var returnType: AstType
    lateinit var name: Name
    lateinit var symbol: AstVariableSymbol<AstEnumEntry>
    var initializer: AstExpression? = null
    val annotations: MutableList<AstFunctionCall> = mutableListOf()

    fun build(): AstEnumEntry {
        return AstEnumEntryImpl(
            origin,
            returnType,
            name,
            symbol,
            initializer,
            annotations,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumEntry(init: AstEnumEntryBuilder.() -> Unit): AstEnumEntry {
    return AstEnumEntryBuilder().apply(init).build()
}
