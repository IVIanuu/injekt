package com.ivianuu.ast.types.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.impl.AstSimpleTypeImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstSimpleTypeBuilder {
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var isMarkedNullable: Boolean = false
    lateinit var classifier: AstClassifierSymbol<*>
    val arguments: MutableList<AstTypeProjection> = mutableListOf()

    fun build(): AstSimpleType {
        return AstSimpleTypeImpl(
            annotations,
            isMarkedNullable,
            classifier,
            arguments,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildSimpleType(init: AstSimpleTypeBuilder.() -> Unit): AstSimpleType {
    return AstSimpleTypeBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstSimpleType.copy(init: AstSimpleTypeBuilder.() -> Unit = {}): AstSimpleType {
    val copyBuilder = AstSimpleTypeBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.isMarkedNullable = isMarkedNullable
    copyBuilder.classifier = classifier
    copyBuilder.arguments.addAll(arguments)
    return copyBuilder.apply(init).build()
}
