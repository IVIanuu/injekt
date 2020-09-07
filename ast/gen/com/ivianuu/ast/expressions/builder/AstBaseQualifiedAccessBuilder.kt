package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstBaseQualifiedAccess
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstBaseQualifiedAccessBuilder : AstBuilder {
    abstract val annotations: MutableList<AstFunctionCall>
    abstract var type: AstType
    abstract val typeArguments: MutableList<AstType>
    abstract var dispatchReceiver: AstExpression?
    abstract var extensionReceiver: AstExpression?
    fun build(): AstBaseQualifiedAccess
}
