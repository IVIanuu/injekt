package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstImplicitThisReference
import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstImplicitThisReferenceBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var boundSymbol: AbstractAstSymbol<*>? = null

    override fun build(): AstThisReference {
        return AstImplicitThisReference(
            type,
            annotations,
            boundSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildImplicitThisReference(init: AstImplicitThisReferenceBuilder.() -> Unit): AstThisReference {
    return AstImplicitThisReferenceBuilder().apply(init).build()
}
