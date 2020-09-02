package com.ivianuu.ast.types.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.types.AstDelegatedType
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstDelegatedTypeImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDelegatedTypeBuilder {
    lateinit var type: AstType
    lateinit var expression: AstExpression

    fun build(): AstDelegatedType {
        return AstDelegatedTypeImpl(
            type,
            expression,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildDelegatedType(init: AstDelegatedTypeBuilder.() -> Unit): AstDelegatedType {
    return AstDelegatedTypeBuilder().apply(init).build()
}
