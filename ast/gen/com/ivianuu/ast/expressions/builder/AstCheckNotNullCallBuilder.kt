package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstCheckNotNullCall
import com.ivianuu.ast.expressions.impl.AstCheckNotNullCallImpl
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.references.impl.AstStubReference
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitTypeRefImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstCheckNotNullCallBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var typeRef: AstTypeRef = AstImplicitTypeRefImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var argumentList: AstArgumentList
    var calleeReference: AstReference = AstStubReference

    override fun build(): AstCheckNotNullCall {
        return AstCheckNotNullCallImpl(
            typeRef,
            annotations,
            argumentList,
            calleeReference,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildCheckNotNullCall(init: AstCheckNotNullCallBuilder.() -> Unit): AstCheckNotNullCall {
    return AstCheckNotNullCallBuilder().apply(init).build()
}
