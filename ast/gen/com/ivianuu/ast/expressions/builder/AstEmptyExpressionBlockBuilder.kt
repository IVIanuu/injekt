package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstEmptyExpressionBlock
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstEmptyExpressionBlockBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstBlock {
        return AstEmptyExpressionBlock(
            type,
        )
    }

    @Deprecated("Modification of 'annotations' has no impact for AstEmptyExpressionBlockBuilder", level = DeprecationLevel.HIDDEN)
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
}

@OptIn(ExperimentalContracts::class)
inline fun buildEmptyExpressionBlock(init: AstEmptyExpressionBlockBuilder.() -> Unit): AstBlock {
    return AstEmptyExpressionBlockBuilder().apply(init).build()
}
