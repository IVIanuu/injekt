package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstBlockImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstBlockBuilder(override val context: AstContext) : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    val statements: MutableList<AstStatement> = mutableListOf()
    override lateinit var type: AstType

    override fun build(): AstBlock {
        return AstBlockImpl(
            context,
            annotations,
            statements,
            type,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildBlock(init: AstBlockBuilder.() -> Unit): AstBlock {
    return AstBlockBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstBlock.copy(init: AstBlockBuilder.() -> Unit = {}): AstBlock {
    val copyBuilder = AstBlockBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.statements.addAll(statements)
    copyBuilder.type = type
    return copyBuilder.apply(init).build()
}
