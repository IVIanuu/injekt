package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.impl.AstExpressionStub
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitTypeRefImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstExpressionStubBuilder : AstAnnotationContainerBuilder {
    var typeRef: AstTypeRef = AstImplicitTypeRefImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstExpression {
        return AstExpressionStub(
            typeRef,
            annotations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildExpressionStub(init: AstExpressionStubBuilder.() -> Unit = {}): AstExpression {
    return AstExpressionStubBuilder().apply(init).build()
}
