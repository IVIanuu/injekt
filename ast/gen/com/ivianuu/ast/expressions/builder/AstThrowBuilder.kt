package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstThrow
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstThrowImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstThrowBuilder(override val context: AstContext) : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var exception: AstExpression

    override fun build(): AstThrow {
        return AstThrowImpl(
            context,
            annotations,
            exception,
        )
    }

    @Deprecated("Modification of 'type' has no impact for AstThrowBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildThrow(init: AstThrowBuilder.() -> Unit): AstThrow {
    return AstThrowBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstThrow.copy(init: AstThrowBuilder.() -> Unit = {}): AstThrow {
    val copyBuilder = AstThrowBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.exception = exception
    return copyBuilder.apply(init).build()
}
