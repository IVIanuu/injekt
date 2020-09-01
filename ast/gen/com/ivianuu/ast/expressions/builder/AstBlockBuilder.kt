package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.impl.AstBlockImpl
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstBlockBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    val statements: MutableList<AstStatement> = mutableListOf()

    override fun build(): AstBlock {
        return AstBlockImpl(
            annotations,
            statements,
        )
    }

    @Deprecated(
        "Modification of 'typeRef' has no impact for AstBlockBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildBlock(init: AstBlockBuilder.() -> Unit = {}): AstBlock {
    return AstBlockBuilder().apply(init).build()
}
