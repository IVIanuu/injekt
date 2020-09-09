package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstDoWhileLoop
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstLoopBuilder
import com.ivianuu.ast.expressions.impl.AstDoWhileLoopImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDoWhileLoopBuilder(override val context: AstContext) : AstLoopBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var condition: AstExpression
    override lateinit var body: AstExpression

    override fun build(): AstDoWhileLoop {
        return AstDoWhileLoopImpl(
            context,
            annotations,
            condition,
            body,
        )
    }

    @Deprecated("Modification of 'type' has no impact for AstDoWhileLoopBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildDoWhileLoop(init: AstDoWhileLoopBuilder.() -> Unit): AstDoWhileLoop {
    return AstDoWhileLoopBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstDoWhileLoop.copy(init: AstDoWhileLoopBuilder.() -> Unit = {}): AstDoWhileLoop {
    val copyBuilder = AstDoWhileLoopBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.condition = condition
    copyBuilder.body = body
    return copyBuilder.apply(init).build()
}
