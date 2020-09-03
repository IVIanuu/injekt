package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstWhileLoop
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstLoopBuilder
import com.ivianuu.ast.expressions.impl.AstWhileLoopImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstWhileLoopBuilder : AstLoopBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    override var label: String? = null
    override lateinit var condition: AstExpression
    override lateinit var body: AstExpression

    override fun build(): AstWhileLoop {
        return AstWhileLoopImpl(
            annotations,
            type,
            label,
            condition,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildWhileLoop(init: AstWhileLoopBuilder.() -> Unit): AstWhileLoop {
    return AstWhileLoopBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstWhileLoop.copy(init: AstWhileLoopBuilder.() -> Unit = {}): AstWhileLoop {
    val copyBuilder = AstWhileLoopBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.label = label
    copyBuilder.condition = condition
    copyBuilder.body = body
    return copyBuilder.apply(init).build()
}
