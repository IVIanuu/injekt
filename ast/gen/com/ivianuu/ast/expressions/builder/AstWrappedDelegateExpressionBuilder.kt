package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstWrappedDelegateExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstWrappedDelegateExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstWrappedDelegateExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var expression: AstExpression
    lateinit var delegateProvider: AstExpression

    override fun build(): AstWrappedDelegateExpression {
        return AstWrappedDelegateExpressionImpl(
            annotations,
            expression,
            delegateProvider,
        )
    }

    @Deprecated("Modification of 'type' has no impact for AstWrappedDelegateExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildWrappedDelegateExpression(init: AstWrappedDelegateExpressionBuilder.() -> Unit): AstWrappedDelegateExpression {
    return AstWrappedDelegateExpressionBuilder().apply(init).build()
}
