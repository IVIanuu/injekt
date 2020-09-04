package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.builder.AstBaseQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstQualifiedAccessImpl
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.utils.lazyVar
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstQualifiedAccessBuilder(override val context: AstContext) : AstBaseQualifiedAccessBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var type: AstType by lazyVar { (callee as? AstCallableSymbol<*>)?.owner?.returnType ?: error("type must be specified") }
    lateinit var callee: AstSymbol<*>
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    override var dispatchReceiver: AstExpression? = null
    override var extensionReceiver: AstExpression? = null

    override fun build(): AstQualifiedAccess {
        return AstQualifiedAccessImpl(
            context,
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
inline fun AstBuilder.buildQualifiedAccess(init: AstQualifiedAccessBuilder.() -> Unit): AstQualifiedAccess {
    return AstQualifiedAccessBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstQualifiedAccess.copy(init: AstQualifiedAccessBuilder.() -> Unit = {}): AstQualifiedAccess {
    val copyBuilder = AstQualifiedAccessBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.callee = callee
    copyBuilder.typeArguments.addAll(typeArguments)
    copyBuilder.dispatchReceiver = dispatchReceiver
    copyBuilder.extensionReceiver = extensionReceiver
    return copyBuilder.apply(init).build()
}
