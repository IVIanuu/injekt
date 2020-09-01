package com.ivianuu.ast.types.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstDynamicTypeRef
import com.ivianuu.ast.types.impl.AstDynamicTypeRefImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDynamicTypeRefBuilder : AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): AstDynamicTypeRef {
        return AstDynamicTypeRefImpl(
            annotations,
            isMarkedNullable,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildDynamicTypeRef(init: AstDynamicTypeRefBuilder.() -> Unit): AstDynamicTypeRef {
    return AstDynamicTypeRefBuilder().apply(init).build()
}
