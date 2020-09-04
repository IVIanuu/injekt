package com.ivianuu.ast.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstSpreadElement
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.impl.AstSpreadElementImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstSpreadElementBuilder(override val context: AstContext) : AstBuilder {
    lateinit var expression: AstExpression

    fun build(): AstSpreadElement {
        return AstSpreadElementImpl(
            context,
            expression,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildSpreadElement(init: AstSpreadElementBuilder.() -> Unit): AstSpreadElement {
    return AstSpreadElementBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstSpreadElement.copy(init: AstSpreadElementBuilder.() -> Unit = {}): AstSpreadElement {
    val copyBuilder = AstSpreadElementBuilder(context)
    copyBuilder.expression = expression
    return copyBuilder.apply(init).build()
}
