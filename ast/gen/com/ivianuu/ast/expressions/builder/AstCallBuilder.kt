package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstCallBuilder {
    abstract val annotations: MutableList<AstCall>
    abstract val arguments: MutableList<AstExpression>

    fun build(): AstCall
}
