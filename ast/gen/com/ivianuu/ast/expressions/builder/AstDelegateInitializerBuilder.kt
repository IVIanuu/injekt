package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstDelegateInitializer
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.impl.AstDelegateInitializerImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDelegateInitializerBuilder(override val context: AstContext) : AstBuilder {
    lateinit var delegatedSuperType: AstType
    lateinit var expression: AstExpression

    fun build(): AstDelegateInitializer {
        return AstDelegateInitializerImpl(
            context,
            delegatedSuperType,
            expression,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildDelegateInitializer(init: AstDelegateInitializerBuilder.() -> Unit): AstDelegateInitializer {
    return AstDelegateInitializerBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstDelegateInitializer.copy(init: AstDelegateInitializerBuilder.() -> Unit = {}): AstDelegateInitializer {
    val copyBuilder = AstDelegateInitializerBuilder(context)
    copyBuilder.delegatedSuperType = delegatedSuperType
    copyBuilder.expression = expression
    return copyBuilder.apply(init).build()
}
