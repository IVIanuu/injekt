package com.ivianuu.ast.builder

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.expressions.AstAnnotationCall

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstAnnotationContainerBuilder {
    abstract val annotations: MutableList<AstAnnotationCall>

    fun build(): AstAnnotationContainer
}
