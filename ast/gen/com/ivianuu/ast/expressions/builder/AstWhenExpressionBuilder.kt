package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.AstWhenExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstWhenExpressionImpl
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.references.impl.AstStubReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstWhenExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var type: AstType = AstImplicitTypeImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    var calleeReference: AstReference = AstStubReference
    var subject: AstExpression? = null
    var subjectVariable: AstVariable<*>? = null
    val branches: MutableList<AstWhenBranch> = mutableListOf()
    var isExhaustive: Boolean = false

    override fun build(): AstWhenExpression {
        return AstWhenExpressionImpl(
            type,
            annotations,
            calleeReference,
            subject,
            subjectVariable,
            branches,
            isExhaustive,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildWhenExpression(init: AstWhenExpressionBuilder.() -> Unit = {}): AstWhenExpression {
    return AstWhenExpressionBuilder().apply(init).build()
}
