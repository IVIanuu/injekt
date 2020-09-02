package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstCall
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
class AstBlockBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstCall> = mutableListOf()
    val statements: MutableList<AstStatement> = mutableListOf()
    override lateinit var type: AstType

    override fun build(): AstBlock {
        return AstBlockImpl(
            annotations,
            statements,
            type,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildBlock(init: AstBlockBuilder.() -> Unit): AstBlock {
    return AstBlockBuilder().apply(init).build()
}
