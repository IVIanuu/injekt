package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstBreak
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstLoopJumpBuilder
import com.ivianuu.ast.expressions.impl.AstBreakImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstBreakBuilder(override val context: AstContext) : AstLoopJumpBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var target: AstTarget<AstLoop>

    override fun build(): AstBreak {
        return AstBreakImpl(
            context,
            annotations,
            target,
        )
    }

    @Deprecated("Modification of 'type' has no impact for AstBreakBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildBreak(init: AstBreakBuilder.() -> Unit): AstBreak {
    return AstBreakBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstBreak.copy(init: AstBreakBuilder.() -> Unit = {}): AstBreak {
    val copyBuilder = AstBreakBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.target = target
    return copyBuilder.apply(init).build()
}
