package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCallableReference
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstBaseQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstCallableReferenceImpl
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstCallableReferenceBuilder(override val context: AstContext) : AstBaseQualifiedAccessBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    override val typeArguments: MutableList<AstType> = mutableListOf()
    override var dispatchReceiver: AstExpression? = null
    override var extensionReceiver: AstExpression? = null
    lateinit var callee: AstCallableSymbol<*>
    var hasQuestionMarkAtLHS: Boolean = false

    override fun build(): AstCallableReference {
        return AstCallableReferenceImpl(
            context,
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
inline fun AstBuilder.buildCallableReference(init: AstCallableReferenceBuilder.() -> Unit): AstCallableReference {
    return AstCallableReferenceBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstCallableReference.copy(init: AstCallableReferenceBuilder.() -> Unit = {}): AstCallableReference {
    val copyBuilder = AstCallableReferenceBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.typeArguments.addAll(typeArguments)
    copyBuilder.dispatchReceiver = dispatchReceiver
    copyBuilder.extensionReceiver = extensionReceiver
    copyBuilder.callee = callee
    copyBuilder.hasQuestionMarkAtLHS = hasQuestionMarkAtLHS
    return copyBuilder.apply(init).build()
}
