package com.ivianuu.ast.references.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.references.AstBackingFieldReference
import com.ivianuu.ast.references.impl.AstBackingFieldReferenceImpl
import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.symbols.impl.AstBackingFieldSymbol
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstBackingFieldReferenceBuilder {
    var candidateSymbol: AbstractAstSymbol<*>? = null
    lateinit var resolvedSymbol: AstBackingFieldSymbol

    fun build(): AstBackingFieldReference {
        return AstBackingFieldReferenceImpl(
            candidateSymbol,
            resolvedSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildBackingFieldReference(init: AstBackingFieldReferenceBuilder.() -> Unit): AstBackingFieldReference {
    return AstBackingFieldReferenceBuilder().apply(init).build()
}
