package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCallableReferenceAccess
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstQualifiedAccessBuilder
import com.ivianuu.ast.expressions.impl.AstCallableReferenceAccessImpl
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
class AstCallableReferenceAccessBuilder : AstQualifiedAccessBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    override var dispatchReceiver: AstExpression? = null
    override var extensionReceiver: AstExpression? = null
    lateinit var callee: AstCallableSymbol<*>
    var hasQuestionMarkAtLHS: Boolean = false

    override fun build(): AstCallableReferenceAccess {
        return AstCallableReferenceAccessImpl(
            type,
            annotations,
            typeArguments,
            dispatchReceiver,
            extensionReceiver,
            callee,
            hasQuestionMarkAtLHS,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCallableReferenceAccess(init: AstCallableReferenceAccessBuilder.() -> Unit): AstCallableReferenceAccess {
    return AstCallableReferenceAccessBuilder().apply(init).build()
}
