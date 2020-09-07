package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstClassReference
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstClassReferenceImpl
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstClassReferenceBuilder(override val context: AstContext) : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    lateinit var classifier: AstClassifierSymbol<*>

    override fun build(): AstClassReference {
        return AstClassReferenceImpl(
            context,
            annotations,
            type,
            classifier,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildClassReference(init: AstClassReferenceBuilder.() -> Unit): AstClassReference {
    return AstClassReferenceBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstClassReference.copy(init: AstClassReferenceBuilder.() -> Unit = {}): AstClassReference {
    val copyBuilder = AstClassReferenceBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.classifier = classifier
    return copyBuilder.apply(init).build()
}
