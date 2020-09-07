package com.ivianuu.ast.types

import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.builder.copy
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.builder.buildType
import com.ivianuu.ast.types.builder.buildTypeProjectionWithVariance
import com.ivianuu.ast.types.builder.copy
import org.jetbrains.kotlin.types.Variance

fun AstType.makeNullable(): AstType {
    if (isMarkedNullable) return this
    return copy { isMarkedNullable = true }
}

fun AstType.makeNotNull(): AstType {
    if (!isMarkedNullable) return this
    return copy { isMarkedNullable = false }
}

fun AstType.typeWith(arguments: List<AstTypeProjection>): AstType {
    return copy {
        this.arguments.clear()
        this.arguments += arguments
    }
}

@JvmName("typeWithTypes")
fun AstType.typeWith(arguments: List<AstType>): AstType = typeWith(
    arguments.map { it.toTypeProjection() }
)

fun AstType.toTypeProjection(variance: Variance = Variance.INVARIANT): AstTypeProjectionWithVariance =
    context.buildTypeProjectionWithVariance {
        this.type = this@toTypeProjection
        this.variance = variance
    }

fun AstType.substitute(params: List<AstTypeParameterSymbol>, arguments: List<AstType>): AstType =
    substitute(params.zip(arguments).toMap())

fun AstType.substitute(substitutionMap: Map<AstTypeParameterSymbol, AstType>): AstType {
    substitutionMap[classifier]?.let { return it }
    return context.buildType {
        this.classifier = this@substitute.classifier
        this.isMarkedNullable = this@substitute.isMarkedNullable
        this.arguments += arguments.map { argument ->
            if (argument is AstTypeProjectionWithVariance) {
                context.buildTypeProjectionWithVariance {
                    type = argument.type.substitute(substitutionMap)
                    variance = argument.variance
                }
            } else {
                argument
            }
        }
        this.annotations += this@substitute.annotations.map { it.copy() }
    }
}
