package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstSuperReference
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstSuperReferenceImpl
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstSuperReferenceBuilder(override val context: AstContext) : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    lateinit var superType: AstClassifierSymbol<*>

    override fun build(): AstSuperReference {
        return AstSuperReferenceImpl(
            context,
            annotations,
            type,
            superType,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildSuperReference(init: AstSuperReferenceBuilder.() -> Unit): AstSuperReference {
    return AstSuperReferenceBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstSuperReference.copy(init: AstSuperReferenceBuilder.() -> Unit = {}): AstSuperReference {
    val copyBuilder = AstSuperReferenceBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.superType = superType
    return copyBuilder.apply(init).build()
}
