package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstVarargArgumentsExpression
import com.ivianuu.ast.expressions.impl.AstVarargArgumentsExpressionImpl
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitTypeRefImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstVarargArgumentsExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var typeRef: AstTypeRef = AstImplicitTypeRefImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    val arguments: MutableList<AstExpression> = mutableListOf()
    lateinit var varargElementType: AstTypeRef

    override fun build(): AstVarargArgumentsExpression {
        return AstVarargArgumentsExpressionImpl(
            typeRef,
            annotations,
            arguments,
            varargElementType,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildVarargArgumentsExpression(init: AstVarargArgumentsExpressionBuilder.() -> Unit): AstVarargArgumentsExpression {
    return AstVarargArgumentsExpressionBuilder().apply(init).build()
}
