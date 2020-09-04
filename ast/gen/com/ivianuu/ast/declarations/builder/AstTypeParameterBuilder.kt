package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.builder.AstNamedDeclarationBuilder
import com.ivianuu.ast.declarations.impl.AstTypeParameterImpl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.utils.lazyVar
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeParameterBuilder(override val context: AstContext) : AstNamedDeclarationBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    override lateinit var name: Name
    var symbol: AstTypeParameterSymbol by lazyVar { AstTypeParameterSymbol() }
    var variance: Variance = Variance.INVARIANT
    var isReified: Boolean = false
    val bounds: MutableList<AstType> = mutableListOf()

    override fun build(): AstTypeParameter {
        return AstTypeParameterImpl(
            context,
            annotations,
            origin,
            name,
            symbol,
            variance,
            isReified,
            bounds,
        )
    }

    @Deprecated("Modification of 'attributes' has no impact for AstTypeParameterBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildTypeParameter(init: AstTypeParameterBuilder.() -> Unit): AstTypeParameter {
    return AstTypeParameterBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstTypeParameter.copy(init: AstTypeParameterBuilder.() -> Unit = {}): AstTypeParameter {
    val copyBuilder = AstTypeParameterBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.name = name
    copyBuilder.symbol = symbol
    copyBuilder.variance = variance
    copyBuilder.isReified = isReified
    copyBuilder.bounds.addAll(bounds)
    return copyBuilder.apply(init).build()
}
