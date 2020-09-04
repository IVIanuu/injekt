package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
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
class AstWhileLoopBuilder(override val context: AstContext) : AstLoopBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var label: String? = null
    override lateinit var condition: AstExpression
    override lateinit var body: AstExpression

    override fun build(): AstWhileLoop {
        return AstWhileLoopImpl(
            context,
            annotations,
            label,
            condition,
            body,
        )
    }


    @Deprecated("Modification of 'type' has no impact for AstWhileLoopBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildWhileLoop(init: AstWhileLoopBuilder.() -> Unit): AstWhileLoop {
    return AstWhileLoopBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstWhileLoop.copy(init: AstWhileLoopBuilder.() -> Unit = {}): AstWhileLoop {
    val copyBuilder = AstWhileLoopBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.label = label
    copyBuilder.condition = condition
    copyBuilder.body = body
    return copyBuilder.apply(init).build()
}
