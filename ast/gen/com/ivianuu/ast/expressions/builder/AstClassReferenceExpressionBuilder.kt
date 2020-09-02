package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstClassReferenceExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstClassReferenceExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstClassReferenceExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstCall> = mutableListOf()
    lateinit var classType: AstType

    override fun build(): AstClassReferenceExpression {
        return AstClassReferenceExpressionImpl(
            type,
            annotations,
            classType,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildClassReferenceExpression(init: AstClassReferenceExpressionBuilder.() -> Unit): AstClassReferenceExpression {
    return AstClassReferenceExpressionBuilder().apply(init).build()
}
