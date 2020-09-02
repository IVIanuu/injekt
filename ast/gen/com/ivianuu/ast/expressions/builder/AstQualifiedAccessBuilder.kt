package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstQualifiedAccessBuilder {
    abstract var type: AstType
    abstract val annotations: MutableList<AstFunctionCall>
    abstract val typeArguments: MutableList<AstTypeProjection>
    abstract var dispatchReceiver: AstExpression?
    abstract var extensionReceiver: AstExpression?
    fun build(): AstQualifiedAccess
}
