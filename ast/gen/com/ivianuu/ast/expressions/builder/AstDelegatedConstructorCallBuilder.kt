package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.impl.AstDelegatedConstructorCallImpl
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDelegatedConstructorCallBuilder : AstCallBuilder, AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstCall> = mutableListOf()
    override val arguments: MutableList<AstExpression> = mutableListOf()
    lateinit var constructedType: AstType
    var dispatchReceiver: AstExpression? = null
    lateinit var calleeReference: AstReference
    var isThis: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): AstDelegatedConstructorCall {
        return AstDelegatedConstructorCallImpl(
            annotations,
            arguments,
            constructedType,
            dispatchReceiver,
            calleeReference,
            isThis,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDelegatedConstructorCall(init: AstDelegatedConstructorCallBuilder.() -> Unit): AstDelegatedConstructorCall {
    return AstDelegatedConstructorCallBuilder().apply(init).build()
}
