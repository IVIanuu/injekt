package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
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
class AstEnumEntryBuilder : AstAnnotationContainerBuilder {
    lateinit var origin: AstDeclarationOrigin
    lateinit var returnType: AstType
    lateinit var name: Name
    lateinit var symbol: AstVariableSymbol<AstEnumEntry>
    var initializer: AstExpression? = null
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var status: AstDeclarationStatus

    override fun build(): AstEnumEntry {
        return AstEnumEntryImpl(
            origin,
            returnType,
            name,
            symbol,
            initializer,
            annotations,
            status,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumEntry(init: AstEnumEntryBuilder.() -> Unit): AstEnumEntry {
    return AstEnumEntryBuilder().apply(init).build()
}
