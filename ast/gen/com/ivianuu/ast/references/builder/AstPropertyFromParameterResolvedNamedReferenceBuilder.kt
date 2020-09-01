package com.ivianuu.ast.references.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.references.AstResolvedNamedReference
import com.ivianuu.ast.references.impl.AstPropertyFromParameterResolvedNamedReference
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstPropertyFromParameterResolvedNamedReferenceBuilder {
    lateinit var name: Name
    lateinit var resolvedSymbol: AbstractAstBasedSymbol<*>

    @OptIn(AstImplementationDetail::class)
    fun build(): AstResolvedNamedReference {
        return AstPropertyFromParameterResolvedNamedReference(
            name,
            resolvedSymbol,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyFromParameterResolvedNamedReference(init: AstPropertyFromParameterResolvedNamedReferenceBuilder.() -> Unit): AstResolvedNamedReference {
    return AstPropertyFromParameterResolvedNamedReferenceBuilder().apply(init).build()
}
