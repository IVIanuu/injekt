package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstVarargElement
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstVarargImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstVarargBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    val elements: MutableList<AstVarargElement> = mutableListOf()

    override fun build(): AstVararg {
        return AstVarargImpl(
            type,
            annotations,
            elements,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildVararg(init: AstVarargBuilder.() -> Unit): AstVararg {
    return AstVarargBuilder().apply(init).build()
}
