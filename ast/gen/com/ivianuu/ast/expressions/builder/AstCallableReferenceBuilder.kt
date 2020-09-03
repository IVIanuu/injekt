package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCallableReference
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstBaseQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstCallableReferenceImpl
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstCallableReferenceBuilder : AstBaseQualifiedAccessBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    override var dispatchReceiver: AstExpression? = null
    override var extensionReceiver: AstExpression? = null
    lateinit var callee: AstCallableSymbol<*>
    var hasQuestionMarkAtLHS: Boolean = false

    override fun build(): AstCallableReference {
        return AstCallableReferenceImpl(
            annotations,
            type,
            typeArguments,
            dispatchReceiver,
            extensionReceiver,
            callee,
            hasQuestionMarkAtLHS,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCallableReference(init: AstCallableReferenceBuilder.() -> Unit): AstCallableReference {
    return AstCallableReferenceBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstCallableReference.copy(init: AstCallableReferenceBuilder.() -> Unit = {}): AstCallableReference {
    val copyBuilder = AstCallableReferenceBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.typeArguments.addAll(typeArguments)
    copyBuilder.dispatchReceiver = dispatchReceiver
    copyBuilder.extensionReceiver = extensionReceiver
    copyBuilder.callee = callee
    copyBuilder.hasQuestionMarkAtLHS = hasQuestionMarkAtLHS
    return copyBuilder.apply(init).build()
}
