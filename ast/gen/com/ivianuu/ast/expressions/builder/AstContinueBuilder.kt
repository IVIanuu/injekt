package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstContinue
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstLoopJumpBuilder
import com.ivianuu.ast.expressions.impl.AstContinueImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstContinueBuilder : AstLoopJumpBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var target: AstTarget<AstLoop>

    override fun build(): AstContinue {
        return AstContinueImpl(
            type,
            annotations,
            target,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildContinue(init: AstContinueBuilder.() -> Unit): AstContinue {
    return AstContinueBuilder().apply(init).build()
}
