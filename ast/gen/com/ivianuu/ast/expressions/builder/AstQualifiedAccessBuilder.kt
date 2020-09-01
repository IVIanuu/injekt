package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.types.AstTypeProjection

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstQualifiedAccessBuilder {
    abstract val annotations: MutableList<AstAnnotationCall>
    abstract val typeArguments: MutableList<AstTypeProjection>
    abstract var explicitReceiver: AstExpression?
    abstract var dispatchReceiver: AstExpression
    abstract var extensionReceiver: AstExpression
    fun build(): AstQualifiedAccess
}
