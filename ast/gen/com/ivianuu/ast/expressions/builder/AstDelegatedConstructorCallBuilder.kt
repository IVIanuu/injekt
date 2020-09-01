package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstEmptyArgumentList
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.impl.AstDelegatedConstructorCallImpl
import com.ivianuu.ast.expressions.impl.AstNoReceiverExpression
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.references.impl.AstExplicitSuperReference
import com.ivianuu.ast.references.impl.AstExplicitThisReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDelegatedConstructorCallBuilder : AstCallBuilder, AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override var argumentList: AstArgumentList = AstEmptyArgumentList
    lateinit var constructedType: AstType
    var dispatchReceiver: AstExpression = AstNoReceiverExpression
    var isThis: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): AstDelegatedConstructorCall {
        return AstDelegatedConstructorCallImpl(
            annotations,
            argumentList,
            constructedType,
            dispatchReceiver,
            isThis,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildDelegatedConstructorCall(init: AstDelegatedConstructorCallBuilder.() -> Unit): AstDelegatedConstructorCall {
    return AstDelegatedConstructorCallBuilder().apply(init).build()
}
