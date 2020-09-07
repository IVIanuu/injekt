package com.ivianuu.ast.types.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.impl.AstTypeImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeBuilder(override val context: AstContext) : AstBuilder {
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var classifier: AstClassifierSymbol<*>
    val arguments: MutableList<AstTypeProjection> = mutableListOf()
    var isMarkedNullable: Boolean = false

    fun build(): AstType {
        return AstTypeImpl(
            context,
            annotations,
            classifier,
            arguments,
            isMarkedNullable,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildType(init: AstTypeBuilder.() -> Unit): AstType {
    return AstTypeBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstType.copy(init: AstTypeBuilder.() -> Unit = {}): AstType {
    val copyBuilder = AstTypeBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.classifier = classifier
    copyBuilder.arguments.addAll(arguments)
    copyBuilder.isMarkedNullable = isMarkedNullable
    return copyBuilder.apply(init).build()
}
