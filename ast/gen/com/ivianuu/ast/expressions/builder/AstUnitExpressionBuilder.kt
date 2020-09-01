package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.impl.AstUnitExpression
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstUnitExpressionBuilder : AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstExpression {
        return AstUnitExpression(
            annotations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildUnitExpression(init: AstUnitExpressionBuilder.() -> Unit = {}): AstExpression {
    return AstUnitExpressionBuilder().apply(init).build()
}
