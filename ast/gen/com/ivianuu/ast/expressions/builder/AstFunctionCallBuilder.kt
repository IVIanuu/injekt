package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstBaseQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstFunctionCallImpl
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstFunctionCallBuilder : AstBaseQualifiedAccessBuilder, AstCallBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    override var dispatchReceiver: AstExpression? = null
    override var extensionReceiver: AstExpression? = null
    open lateinit var callee: AstFunctionSymbol<*>
    override val valueArguments: MutableList<AstExpression?> = mutableListOf()

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstFunctionCall {
        return AstFunctionCallImpl(
            annotations,
            type,
            typeArguments,
            dispatchReceiver,
            extensionReceiver,
            callee,
            valueArguments,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionCall(init: AstFunctionCallBuilder.() -> Unit): AstFunctionCall {
    return AstFunctionCallBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstFunctionCall.copy(init: AstFunctionCallBuilder.() -> Unit = {}): AstFunctionCall {
    val copyBuilder = AstFunctionCallBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.typeArguments.addAll(typeArguments)
    copyBuilder.dispatchReceiver = dispatchReceiver
    copyBuilder.extensionReceiver = extensionReceiver
    copyBuilder.callee = callee
    copyBuilder.valueArguments.addAll(valueArguments)
    return copyBuilder.apply(init).build()
}
