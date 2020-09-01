package com.ivianuu.ast.references.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.references.AstDelegateFieldReference
import com.ivianuu.ast.references.impl.AstDelegateFieldReferenceImpl
import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.symbols.impl.AstDelegateFieldSymbol
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDelegateFieldReferenceBuilder {
    var candidateSymbol: AbstractAstSymbol<*>? = null
    lateinit var resolvedSymbol: AstDelegateFieldSymbol<*>

    fun build(): AstDelegateFieldReference {
        return AstDelegateFieldReferenceImpl(
            candidateSymbol,
            resolvedSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDelegateFieldReference(init: AstDelegateFieldReferenceBuilder.() -> Unit): AstDelegateFieldReference {
    return AstDelegateFieldReferenceBuilder().apply(init).build()
}
