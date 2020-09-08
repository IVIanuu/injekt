package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.impl.AstEnumEntryImpl
import com.ivianuu.ast.expressions.AstDelegateInitializer
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.symbols.impl.AstEnumEntrySymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.utils.lazyVar
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstEnumEntryBuilder(override val context: AstContext) : AstBuilder {
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    var attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    val declarations: MutableList<AstDeclaration> = mutableListOf()
    val delegateInitializers: MutableList<AstDelegateInitializer> = mutableListOf()
    var name: Name by lazyVar { symbol.fqName.shortName() }
    lateinit var symbol: AstEnumEntrySymbol

    fun build(): AstEnumEntry {
        return AstEnumEntryImpl(
            context,
            annotations,
            origin,
            attributes,
            declarations,
            delegateInitializers,
            name,
            symbol,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildEnumEntry(init: AstEnumEntryBuilder.() -> Unit): AstEnumEntry {
    return AstEnumEntryBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstEnumEntry.copy(init: AstEnumEntryBuilder.() -> Unit = {}): AstEnumEntry {
    val copyBuilder = AstEnumEntryBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.attributes = attributes
    copyBuilder.declarations.addAll(declarations)
    copyBuilder.delegateInitializers.addAll(delegateInitializers)
    copyBuilder.name = name
    copyBuilder.symbol = symbol
    return copyBuilder.apply(init).build()
}
