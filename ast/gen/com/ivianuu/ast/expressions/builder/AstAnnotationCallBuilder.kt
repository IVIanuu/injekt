package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstEmptyArgumentList
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstAnnotationCallImpl
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAnnotationCallBuilder : AstCallBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override var argumentList: AstArgumentList = AstEmptyArgumentList
    lateinit var calleeReference: AstReference
    lateinit var annotationType: AstType

    override fun build(): AstAnnotationCall {
        return AstAnnotationCallImpl(
            annotations,
            argumentList,
            calleeReference,
            annotationType,
        )
    }

    @Deprecated("Modification of 'type' has no impact for AstAnnotationCallBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotationCall(init: AstAnnotationCallBuilder.() -> Unit): AstAnnotationCall {
    return AstAnnotationCallBuilder().apply(init).build()
}
