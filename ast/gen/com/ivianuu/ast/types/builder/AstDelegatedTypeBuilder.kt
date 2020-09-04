package com.ivianuu.ast.types.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
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
class AstDelegatedTypeBuilder(override val context: AstContext) : AstBuilder {
    lateinit var type: AstType
    lateinit var expression: AstExpression

    fun build(): AstDelegatedType {
        return AstDelegatedTypeImpl(
            context,
            type,
            expression,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildDelegatedType(init: AstDelegatedTypeBuilder.() -> Unit): AstDelegatedType {
    return AstDelegatedTypeBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstDelegatedType.copy(init: AstDelegatedTypeBuilder.() -> Unit = {}): AstDelegatedType {
    val copyBuilder = AstDelegatedTypeBuilder(context)
    copyBuilder.type = type
    copyBuilder.expression = expression
    return copyBuilder.apply(init).build()
}
