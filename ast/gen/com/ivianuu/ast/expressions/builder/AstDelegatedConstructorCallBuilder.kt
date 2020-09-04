package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstDelegatedConstructorCallKind
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstDelegatedConstructorCallImpl
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDelegatedConstructorCallBuilder(override val context: AstContext) : AstCallBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    override val valueArguments: MutableList<AstExpression?> = mutableListOf()
    lateinit var callee: AstConstructorSymbol
    var dispatchReceiver: AstExpression? = null
    lateinit var kind: AstDelegatedConstructorCallKind

    override fun build(): AstDelegatedConstructorCall {
        return AstDelegatedConstructorCallImpl(
            context,
            annotations,
            type,
            valueArguments,
            callee,
            dispatchReceiver,
            kind,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildDelegatedConstructorCall(init: AstDelegatedConstructorCallBuilder.() -> Unit): AstDelegatedConstructorCall {
    return AstDelegatedConstructorCallBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstDelegatedConstructorCall.copy(init: AstDelegatedConstructorCallBuilder.() -> Unit = {}): AstDelegatedConstructorCall {
    val copyBuilder = AstDelegatedConstructorCallBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.valueArguments.addAll(valueArguments)
    copyBuilder.callee = callee
    copyBuilder.dispatchReceiver = dispatchReceiver
    copyBuilder.kind = kind
    return copyBuilder.apply(init).build()
}
