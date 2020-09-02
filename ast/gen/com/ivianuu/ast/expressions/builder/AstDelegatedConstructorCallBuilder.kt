package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstDelegatedConstructorCallImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDelegatedConstructorCallBuilder : AstCallBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val valueArguments: MutableList<AstExpression> = mutableListOf()
    lateinit var constructedType: AstType
    var dispatchReceiver: AstExpression? = null
    var isThis: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): AstDelegatedConstructorCall {
        return AstDelegatedConstructorCallImpl(
            type,
            annotations,
            valueArguments,
            constructedType,
            dispatchReceiver,
            isThis,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDelegatedConstructorCall(init: AstDelegatedConstructorCallBuilder.() -> Unit): AstDelegatedConstructorCall {
    return AstDelegatedConstructorCallBuilder().apply(init).build()
}
