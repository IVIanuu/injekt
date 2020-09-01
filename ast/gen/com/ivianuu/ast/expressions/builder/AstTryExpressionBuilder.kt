package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.expressions.AstTryExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstTryExpressionImpl
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
class AstTryExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var type: AstType = AstImplicitTypeImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    var calleeReference: AstReference = AstStubReference
    lateinit var tryBlock: AstBlock
    val catches: MutableList<AstCatch> = mutableListOf()
    var finallyBlock: AstBlock? = null

    override fun build(): AstTryExpression {
        return AstTryExpressionImpl(
            type,
            annotations,
            calleeReference,
            tryBlock,
            catches,
            finallyBlock,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTryExpression(init: AstTryExpressionBuilder.() -> Unit): AstTryExpression {
    return AstTryExpressionBuilder().apply(init).build()
}
