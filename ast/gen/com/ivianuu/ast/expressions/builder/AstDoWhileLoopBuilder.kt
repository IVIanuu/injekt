package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstDoWhileLoop
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.builder.AstLoopBuilder
import com.ivianuu.ast.expressions.impl.AstDoWhileLoopImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDoWhileLoopBuilder : AstLoopBuilder, AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstCall> = mutableListOf()
    override lateinit var block: AstBlock
    override lateinit var condition: AstExpression
    override var label: AstLabel? = null

    override fun build(): AstDoWhileLoop {
        return AstDoWhileLoopImpl(
            annotations,
            block,
            condition,
            label,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDoWhileLoop(init: AstDoWhileLoopBuilder.() -> Unit): AstDoWhileLoop {
    return AstDoWhileLoopBuilder().apply(init).build()
}
