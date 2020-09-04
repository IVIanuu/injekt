package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstTry
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstTryImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTryBuilder(override val context: AstContext) : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    lateinit var tryBody: AstExpression
    val catches: MutableList<AstCatch> = mutableListOf()
    var finallyBody: AstExpression? = null

    override fun build(): AstTry {
        return AstTryImpl(
            context,
            annotations,
            type,
            tryBody,
            catches,
            finallyBody,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildTry(init: AstTryBuilder.() -> Unit): AstTry {
    return AstTryBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstTry.copy(init: AstTryBuilder.() -> Unit = {}): AstTry {
    val copyBuilder = AstTryBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.tryBody = tryBody
    copyBuilder.catches.addAll(catches)
    copyBuilder.finallyBody = finallyBody
    return copyBuilder.apply(init).build()
}
