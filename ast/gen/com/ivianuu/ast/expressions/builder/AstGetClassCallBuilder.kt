package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstGetClassCall
import com.ivianuu.ast.expressions.impl.AstGetClassCallImpl
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstGetClassCallBuilder : AstCallBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override lateinit var argumentList: AstArgumentList

    override fun build(): AstGetClassCall {
        return AstGetClassCallImpl(
            annotations,
            argumentList,
        )
    }

    @Deprecated("Modification of 'typeRef' has no impact for AstGetClassCallBuilder", level = DeprecationLevel.HIDDEN)
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildGetClassCall(init: AstGetClassCallBuilder.() -> Unit): AstGetClassCall {
    return AstGetClassCallBuilder().apply(init).build()
}
