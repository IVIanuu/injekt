package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstDoWhileLoop
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstLoopBuilder
import com.ivianuu.ast.expressions.impl.AstDoWhileLoopImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDoWhileLoopBuilder : AstLoopBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var body: AstExpression
    override lateinit var condition: AstExpression
    override var label: String? = null

    override fun build(): AstDoWhileLoop {
        return AstDoWhileLoopImpl(
            annotations,
            body,
            condition,
            label,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDoWhileLoop(init: AstDoWhileLoopBuilder.() -> Unit): AstDoWhileLoop {
    return AstDoWhileLoopBuilder().apply(init).build()
}
