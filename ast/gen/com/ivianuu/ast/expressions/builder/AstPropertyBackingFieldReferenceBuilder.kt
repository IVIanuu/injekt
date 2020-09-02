package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstPropertyBackingFieldReference
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstPropertyBackingFieldReferenceImpl
import com.ivianuu.ast.symbols.impl.AstBackingFieldSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstPropertyBackingFieldReferenceBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var resolvedSymbol: AstBackingFieldSymbol

    override fun build(): AstPropertyBackingFieldReference {
        return AstPropertyBackingFieldReferenceImpl(
            type,
            annotations,
            resolvedSymbol,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyBackingFieldReference(init: AstPropertyBackingFieldReferenceBuilder.() -> Unit): AstPropertyBackingFieldReference {
    return AstPropertyBackingFieldReferenceBuilder().apply(init).build()
}
