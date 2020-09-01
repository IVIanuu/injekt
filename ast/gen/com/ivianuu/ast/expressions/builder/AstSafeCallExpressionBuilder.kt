package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstExpressionRef
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstCheckedSafeCallSubject
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstSafeCallExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstSafeCallExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstSafeCallExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var type: AstType = AstImplicitTypeImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var receiver: AstExpression
    lateinit var checkedSubjectRef: AstExpressionRef<AstCheckedSafeCallSubject>
    lateinit var regularQualifiedAccess: AstQualifiedAccess

    override fun build(): AstSafeCallExpression {
        return AstSafeCallExpressionImpl(
            type,
            annotations,
            receiver,
            checkedSubjectRef,
            regularQualifiedAccess,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildSafeCallExpression(init: AstSafeCallExpressionBuilder.() -> Unit): AstSafeCallExpression {
    return AstSafeCallExpressionBuilder().apply(init).build()
}
