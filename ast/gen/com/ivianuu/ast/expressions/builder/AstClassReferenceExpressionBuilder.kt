package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstClassReferenceExpression
import com.ivianuu.ast.expressions.impl.AstClassReferenceExpressionImpl
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstClassReferenceExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var classTypeRef: AstTypeRef

    override fun build(): AstClassReferenceExpression {
        return AstClassReferenceExpressionImpl(
            annotations,
            classTypeRef,
        )
    }

    @Deprecated(
        "Modification of 'typeRef' has no impact for AstClassReferenceExpressionBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildClassReferenceExpression(init: AstClassReferenceExpressionBuilder.() -> Unit): AstClassReferenceExpression {
    return AstClassReferenceExpressionBuilder().apply(init).build()
}
