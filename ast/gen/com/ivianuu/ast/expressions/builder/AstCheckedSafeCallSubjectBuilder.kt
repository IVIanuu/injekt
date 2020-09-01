package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstExpressionRef
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstCheckedSafeCallSubject
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.impl.AstCheckedSafeCallSubjectImpl
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitTypeRefImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstCheckedSafeCallSubjectBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var typeRef: AstTypeRef = AstImplicitTypeRefImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var originalReceiverRef: AstExpressionRef<AstExpression>

    override fun build(): AstCheckedSafeCallSubject {
        return AstCheckedSafeCallSubjectImpl(
            typeRef,
            annotations,
            originalReceiverRef,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildCheckedSafeCallSubject(init: AstCheckedSafeCallSubjectBuilder.() -> Unit): AstCheckedSafeCallSubject {
    return AstCheckedSafeCallSubjectBuilder().apply(init).build()
}
