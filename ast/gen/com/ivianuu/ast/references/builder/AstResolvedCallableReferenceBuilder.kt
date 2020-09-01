package com.ivianuu.ast.references.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.references.AstResolvedCallableReference
import com.ivianuu.ast.references.impl.AstResolvedCallableReferenceImpl
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstResolvedCallableReferenceBuilder {
    lateinit var name: Name
    lateinit var resolvedSymbol: AbstractAstBasedSymbol<*>
    val inferredTypeArguments: MutableList<ConeKotlinType> = mutableListOf()

    fun build(): AstResolvedCallableReference {
        return AstResolvedCallableReferenceImpl(
            name,
            resolvedSymbol,
            inferredTypeArguments,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedCallableReference(init: AstResolvedCallableReferenceBuilder.() -> Unit): AstResolvedCallableReference {
    return AstResolvedCallableReferenceBuilder().apply(init).build()
}
