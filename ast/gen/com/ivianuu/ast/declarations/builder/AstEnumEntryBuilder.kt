package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.impl.AstEnumEntryImpl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
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
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    lateinit var returnType: AstType
    lateinit var name: Name
    lateinit var symbol: AstVariableSymbol<AstEnumEntry>
    var initializer: AstExpression? = null

    fun build(): AstEnumEntry {
        return AstEnumEntryImpl(
            annotations,
            origin,
            returnType,
            name,
            symbol,
            initializer,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumEntry(init: AstEnumEntryBuilder.() -> Unit): AstEnumEntry {
    return AstEnumEntryBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstEnumEntry.copy(init: AstEnumEntryBuilder.() -> Unit = {}): AstEnumEntry {
    val copyBuilder = AstEnumEntryBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.returnType = returnType
    copyBuilder.name = name
    copyBuilder.symbol = symbol
    copyBuilder.initializer = initializer
    return copyBuilder.apply(init).build()
}
