package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstEmptyArgumentList
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.AstTypeOperatorCall
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstTypeOperatorCallImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeOperatorCallBuilder : AstCallBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override var argumentList: AstArgumentList = AstEmptyArgumentList
    lateinit var operation: AstOperation
    lateinit var conversionType: AstType

    override fun build(): AstTypeOperatorCall {
        return AstTypeOperatorCallImpl(
            annotations,
            argumentList,
            operation,
            conversionType,
        )
    }

    @Deprecated("Modification of 'type' has no impact for AstTypeOperatorCallBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeOperatorCall(init: AstTypeOperatorCallBuilder.() -> Unit): AstTypeOperatorCall {
    return AstTypeOperatorCallBuilder().apply(init).build()
}
