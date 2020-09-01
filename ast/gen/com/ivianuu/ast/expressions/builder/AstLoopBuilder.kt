package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstLoop

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstLoopBuilder {
    abstract val annotations: MutableList<AstAnnotationCall>
    abstract var block: AstBlock
    abstract var condition: AstExpression
    abstract var label: AstLabel?
    fun build(): AstLoop
}
