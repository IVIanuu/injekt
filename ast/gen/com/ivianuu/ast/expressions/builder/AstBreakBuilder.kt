package com.ivianuu.ast.expressions.builder

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
class AstBreakBuilder : AstLoopJumpBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    override lateinit var target: AstLoop

    override fun build(): AstBreak {
        return AstBreakImpl(
            annotations,
            type,
            target,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildBreak(init: AstBreakBuilder.() -> Unit): AstBreak {
    return AstBreakBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstBreak.copy(init: AstBreakBuilder.() -> Unit = {}): AstBreak {
    val copyBuilder = AstBreakBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.target = target
    return copyBuilder.apply(init).build()
}
