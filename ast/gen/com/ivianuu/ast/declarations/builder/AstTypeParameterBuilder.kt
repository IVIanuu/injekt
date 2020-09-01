package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.impl.AstTypeParameterImpl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeParameterBuilder : AstAnnotationContainerBuilder {
    lateinit var origin: AstDeclarationOrigin
    lateinit var name: Name
    lateinit var symbol: AstTypeParameterSymbol
    lateinit var variance: Variance
    var isReified: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val bounds: MutableList<AstTypeRef> = mutableListOf()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()

    override fun build(): AstTypeParameter {
        return AstTypeParameterImpl(
            origin,
            name,
            symbol,
            variance,
            isReified,
            bounds,
            annotations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeParameter(init: AstTypeParameterBuilder.() -> Unit): AstTypeParameter {
    return AstTypeParameterBuilder().apply(init).build()
}
