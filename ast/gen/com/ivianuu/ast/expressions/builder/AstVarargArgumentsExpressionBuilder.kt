package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstVarargArgumentsExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstVarargArgumentsExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstVarargArgumentsExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstCall> = mutableListOf()
    val arguments: MutableList<AstExpression> = mutableListOf()
    lateinit var varargElementType: AstType

    override fun build(): AstVarargArgumentsExpression {
        return AstVarargArgumentsExpressionImpl(
            type,
            annotations,
            arguments,
            varargElementType,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildVarargArgumentsExpression(init: AstVarargArgumentsExpressionBuilder.() -> Unit): AstVarargArgumentsExpression {
    return AstVarargArgumentsExpressionBuilder().apply(init).build()
}
