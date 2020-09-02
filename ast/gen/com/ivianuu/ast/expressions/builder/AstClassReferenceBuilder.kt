package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstClassReference
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstClassReferenceImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstClassReferenceBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var classType: AstType

    override fun build(): AstClassReference {
        return AstClassReferenceImpl(
            type,
            annotations,
            classType,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildClassReference(init: AstClassReferenceBuilder.() -> Unit): AstClassReference {
    return AstClassReferenceBuilder().apply(init).build()
}
