package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstThisReferenceImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstThisReferenceBuilder(override val context: AstContext) : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    lateinit var target: AstTarget<*>

    override fun build(): AstThisReference {
        return AstThisReferenceImpl(
            context,
            annotations,
            type,
            target,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildThisReference(init: AstThisReferenceBuilder.() -> Unit): AstThisReference {
    return AstThisReferenceBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstThisReference.copy(init: AstThisReferenceBuilder.() -> Unit = {}): AstThisReference {
    val copyBuilder = AstThisReferenceBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.target = target
    return copyBuilder.apply(init).build()
}
