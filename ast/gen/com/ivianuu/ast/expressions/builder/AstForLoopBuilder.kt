package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstForLoop
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstLoopBuilder
import com.ivianuu.ast.expressions.impl.AstForLoopImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstForLoopBuilder(override val context: AstContext) : AstLoopBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var body: AstExpression
    lateinit var loopRange: AstExpression
    lateinit var loopParameter: AstProperty

    override fun build(): AstForLoop {
        return AstForLoopImpl(
            context,
            annotations,
            body,
            loopRange,
            loopParameter,
        )
    }

    @Deprecated("Modification of 'type' has no impact for AstForLoopBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildForLoop(init: AstForLoopBuilder.() -> Unit): AstForLoop {
    return AstForLoopBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstForLoop.copy(init: AstForLoopBuilder.() -> Unit = {}): AstForLoop {
    val copyBuilder = AstForLoopBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.body = body
    copyBuilder.loopRange = loopRange
    copyBuilder.loopParameter = loopParameter
    return copyBuilder.apply(init).build()
}
