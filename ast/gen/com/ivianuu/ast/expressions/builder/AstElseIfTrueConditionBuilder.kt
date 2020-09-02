package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.impl.AstElseIfTrueCondition
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstElseIfTrueConditionBuilder : AstAnnotationContainerBuilder {
    lateinit var type: AstType
    override val annotations: MutableList<AstCall> = mutableListOf()

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstExpression {
        return AstElseIfTrueCondition(
            type,
            annotations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildElseIfTrueCondition(init: AstElseIfTrueConditionBuilder.() -> Unit): AstExpression {
    return AstElseIfTrueConditionBuilder().apply(init).build()
}
