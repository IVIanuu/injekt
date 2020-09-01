package com.ivianuu.ast.types.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
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
class AstSimpleTypeBuilder : AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var classifier: AstClassifierSymbol<*>
    val arguments: MutableList<AstTypeProjection> = mutableListOf()

    override fun build(): AstSimpleType {
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
