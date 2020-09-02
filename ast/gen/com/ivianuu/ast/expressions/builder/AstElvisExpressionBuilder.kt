package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstElvisExpression
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstElvisExpressionImpl
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.references.impl.AstStubReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstElvisExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstCall> = mutableListOf()
    var calleeReference: AstReference = AstStubReference
    lateinit var lhs: AstExpression
    lateinit var rhs: AstExpression

    override fun build(): AstElvisExpression {
        return AstElvisExpressionImpl(
            type,
            annotations,
            calleeReference,
            lhs,
            rhs,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildElvisExpression(init: AstElvisExpressionBuilder.() -> Unit): AstElvisExpression {
    return AstElvisExpressionBuilder().apply(init).build()
}
