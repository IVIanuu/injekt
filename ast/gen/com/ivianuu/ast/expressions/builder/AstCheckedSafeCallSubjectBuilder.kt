package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstExpressionRef
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstCheckedSafeCallSubject
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstCheckedSafeCallSubjectImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstCheckedSafeCallSubjectBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var type: AstType = AstImplicitTypeImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var originalReceiverRef: AstExpressionRef<AstExpression>

    override fun build(): AstCheckedSafeCallSubject {
        return AstCheckedSafeCallSubjectImpl(
            type,
            annotations,
            originalReceiverRef,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildCheckedSafeCallSubject(init: AstCheckedSafeCallSubjectBuilder.() -> Unit): AstCheckedSafeCallSubject {
    return AstCheckedSafeCallSubjectBuilder().apply(init).build()
}
