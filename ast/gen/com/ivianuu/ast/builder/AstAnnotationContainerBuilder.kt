package com.ivianuu.ast.builder

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstAnnotationContainerBuilder {
    abstract val annotations: MutableList<AstFunctionCall>

    fun build(): AstAnnotationContainer
}
