package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstVarargElement
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstVarargImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstVarargBuilder(override val context: AstContext) : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    val elements: MutableList<AstVarargElement> = mutableListOf()

    override fun build(): AstVararg {
        return AstVarargImpl(
            context,
            annotations,
            type,
            elements,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildVararg(init: AstVarargBuilder.() -> Unit): AstVararg {
    return AstVarargBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstVararg.copy(init: AstVarargBuilder.() -> Unit = {}): AstVararg {
    val copyBuilder = AstVarargBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.elements.addAll(elements)
    return copyBuilder.apply(init).build()
}
