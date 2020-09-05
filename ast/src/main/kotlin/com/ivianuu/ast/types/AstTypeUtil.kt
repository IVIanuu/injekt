package com.ivianuu.ast.types

import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.builder.copy
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.builder.buildSimpleType
import com.ivianuu.ast.types.builder.buildTypeProjectionWithVariance
import com.ivianuu.ast.types.builder.copy

fun AstType.makeNullable(): AstType {
    if (isMarkedNullable) return this
    check(this is AstSimpleType)
    return copy { isMarkedNullable = true }
}

fun AstType.makeNotNull(): AstType {
    if (!isMarkedNullable) return this
    check(this is AstSimpleType)
    return copy { isMarkedNullable = false }
}

fun AstType.substitute(params: List<AstTypeParameterSymbol>, arguments: List<AstType>): AstType =
    substitute(params.zip(arguments).toMap())

fun AstType.substitute(substitutionMap: Map<AstTypeParameterSymbol, AstType>): AstType {
    if (this !is AstSimpleType) return this
    substitutionMap[classifier]?.let { return it }
    return context.buildSimpleType {
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
