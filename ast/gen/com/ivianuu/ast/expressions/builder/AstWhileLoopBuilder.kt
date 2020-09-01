package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstWhileLoop
import com.ivianuu.ast.expressions.builder.AstLoopBuilder
import com.ivianuu.ast.expressions.impl.AstWhileLoopImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstWhileLoopBuilder : AstLoopBuilder, AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override var label: AstLabel? = null
    override lateinit var condition: AstExpression
    override lateinit var block: AstBlock

    override fun build(): AstWhileLoop {
        return AstWhileLoopImpl(
            annotations,
            label,
            condition,
            block,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildWhileLoop(init: AstWhileLoopBuilder.() -> Unit): AstWhileLoop {
    return AstWhileLoopBuilder().apply(init).build()
}
