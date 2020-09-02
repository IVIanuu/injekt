package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstQualifiedAccessExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstQualifiedAccessBuilder
import com.ivianuu.ast.expressions.impl.AstQualifiedAccessExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstQualifiedAccessExpressionBuilder : AstQualifiedAccessBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    override var dispatchReceiver: AstExpression? = null
    override var extensionReceiver: AstExpression? = null

    override fun build(): AstQualifiedAccessExpression {
        return AstQualifiedAccessExpressionImpl(
            type,
            annotations,
            typeArguments,
            dispatchReceiver,
            extensionReceiver,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildQualifiedAccessExpression(init: AstQualifiedAccessExpressionBuilder.() -> Unit): AstQualifiedAccessExpression {
    return AstQualifiedAccessExpressionBuilder().apply(init).build()
}
