package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.impl.AstCatchImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstCatchBuilder {
    lateinit var parameter: AstValueParameter
    lateinit var body: AstExpression

    fun build(): AstCatch {
        return AstCatchImpl(
            parameter,
            body,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildCatch(init: AstCatchBuilder.() -> Unit): AstCatch {
    return AstCatchBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstCatch.copy(init: AstCatchBuilder.() -> Unit = {}): AstCatch {
    val copyBuilder = AstCatchBuilder()
    copyBuilder.parameter = parameter
    copyBuilder.body = body
    return copyBuilder.apply(init).build()
}
