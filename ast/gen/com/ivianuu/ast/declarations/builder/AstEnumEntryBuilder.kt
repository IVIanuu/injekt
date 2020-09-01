package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.declarations.impl.AstEnumEntryImpl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstTypeRef
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstEnumEntryBuilder : AstAnnotationContainerBuilder {
    lateinit var origin: AstDeclarationOrigin
    lateinit var returnTypeRef: AstTypeRef
    lateinit var name: Name
    lateinit var symbol: AstVariableSymbol<AstEnumEntry>
    var initializer: AstExpression? = null
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    val typeParameters: MutableList<AstTypeParameterRef> = mutableListOf()
    lateinit var status: AstDeclarationStatus

    override fun build(): AstEnumEntry {
        return AstEnumEntryImpl(
            origin,
            returnTypeRef,
            name,
            symbol,
            initializer,
            annotations,
            typeParameters,
            status,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumEntry(init: AstEnumEntryBuilder.() -> Unit): AstEnumEntry {
    return AstEnumEntryBuilder().apply(init).build()
}
