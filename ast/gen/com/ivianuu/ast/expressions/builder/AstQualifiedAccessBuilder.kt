package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.builder.AstBaseQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstQualifiedAccessImpl
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstQualifiedAccessBuilder : AstBaseQualifiedAccessBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    lateinit var callee: AstSymbol<*>
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    override var dispatchReceiver: AstExpression? = null
    override var extensionReceiver: AstExpression? = null

    override fun build(): AstQualifiedAccess {
        return AstQualifiedAccessImpl(
            annotations,
            type,
            callee,
            typeArguments,
            dispatchReceiver,
            extensionReceiver,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildQualifiedAccess(init: AstQualifiedAccessBuilder.() -> Unit): AstQualifiedAccess {
    return AstQualifiedAccessBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstQualifiedAccess.copy(init: AstQualifiedAccessBuilder.() -> Unit = {}): AstQualifiedAccess {
    val copyBuilder = AstQualifiedAccessBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.callee = callee
    copyBuilder.typeArguments.addAll(typeArguments)
    copyBuilder.dispatchReceiver = dispatchReceiver
    copyBuilder.extensionReceiver = extensionReceiver
    return copyBuilder.apply(init).build()
}
