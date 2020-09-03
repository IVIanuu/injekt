package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.impl.AstTypeParameterImpl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeParameterBuilder {
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    lateinit var name: Name
    lateinit var symbol: AstTypeParameterSymbol
    var variance: Variance = Variance.INVARIANT
    var isReified: Boolean = false
    val bounds: MutableList<AstType> = mutableListOf()

    fun build(): AstTypeParameter {
        return AstTypeParameterImpl(
            annotations,
            origin,
            name,
            symbol,
            variance,
            isReified,
            bounds,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeParameter(init: AstTypeParameterBuilder.() -> Unit): AstTypeParameter {
    return AstTypeParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstTypeParameter.copy(init: AstTypeParameterBuilder.() -> Unit = {}): AstTypeParameter {
    val copyBuilder = AstTypeParameterBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.name = name
    copyBuilder.symbol = symbol
    copyBuilder.variance = variance
    copyBuilder.isReified = isReified
    copyBuilder.bounds.addAll(bounds)
    return copyBuilder.apply(init).build()
}
