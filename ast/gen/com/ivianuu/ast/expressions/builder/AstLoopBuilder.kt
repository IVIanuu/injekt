package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstLoopBuilder {
    abstract val annotations: MutableList<AstFunctionCall>
    abstract var body: AstExpression
    abstract var condition: AstExpression
    abstract var label: String?
    fun build(): AstLoop
}
