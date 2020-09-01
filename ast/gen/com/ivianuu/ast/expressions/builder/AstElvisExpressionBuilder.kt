package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstElvisExpression
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.impl.AstElvisExpressionImpl
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.references.impl.AstStubReference
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstElvisExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    var calleeReference: AstReference = AstStubReference
    lateinit var lhs: AstExpression
    lateinit var rhs: AstExpression

    override fun build(): AstElvisExpression {
        return AstElvisExpressionImpl(
            annotations,
            calleeReference,
            lhs,
            rhs,
        )
    }

    @Deprecated("Modification of 'typeRef' has no impact for AstElvisExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildElvisExpression(init: AstElvisExpressionBuilder.() -> Unit): AstElvisExpression {
    return AstElvisExpressionBuilder().apply(init).build()
}
