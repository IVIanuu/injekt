package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstReturnImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstReturnBuilder(override val context: AstContext) : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var target: AstTarget<AstFunction<*>>
    lateinit var result: AstExpression

    override fun build(): AstReturn {
        return AstReturnImpl(
            context,
            annotations,
            target,
            result,
        )
    }

    @Deprecated("Modification of 'type' has no impact for AstReturnBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildReturn(init: AstReturnBuilder.() -> Unit): AstReturn {
    return AstReturnBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstReturn.copy(init: AstReturnBuilder.() -> Unit = {}): AstReturn {
    val copyBuilder = AstReturnBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.target = target
    copyBuilder.result = result
    return copyBuilder.apply(init).build()
}
