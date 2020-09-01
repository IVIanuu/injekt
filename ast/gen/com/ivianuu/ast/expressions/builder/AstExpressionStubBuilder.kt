package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.impl.AstExpressionStub
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstExpressionStubBuilder : AstAnnotationContainerBuilder {
    var type: AstType = AstImplicitTypeImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstExpression {
        return AstExpressionStub(
            type,
            annotations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildExpressionStub(init: AstExpressionStubBuilder.() -> Unit = {}): AstExpression {
    return AstExpressionStubBuilder().apply(init).build()
}
