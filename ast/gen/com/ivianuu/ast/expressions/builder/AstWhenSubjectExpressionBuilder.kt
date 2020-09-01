package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstExpressionRef
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstWhenExpression
import com.ivianuu.ast.expressions.AstWhenSubjectExpression
import com.ivianuu.ast.expressions.impl.AstWhenSubjectExpressionImpl
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstWhenSubjectExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var whenRef: AstExpressionRef<AstWhenExpression>

    override fun build(): AstWhenSubjectExpression {
        return AstWhenSubjectExpressionImpl(
            annotations,
            whenRef,
        )
    }

    @Deprecated(
        "Modification of 'typeRef' has no impact for AstWhenSubjectExpressionBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildWhenSubjectExpression(init: AstWhenSubjectExpressionBuilder.() -> Unit): AstWhenSubjectExpression {
    return AstWhenSubjectExpressionBuilder().apply(init).build()
}
