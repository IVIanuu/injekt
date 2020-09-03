package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstPropertyBackingFieldReference
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstPropertyBackingFieldReferenceImpl
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstPropertyBackingFieldReferenceBuilder : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    lateinit var property: AstPropertySymbol

    override fun build(): AstPropertyBackingFieldReference {
        return AstPropertyBackingFieldReferenceImpl(
            annotations,
            type,
            property,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyBackingFieldReference(init: AstPropertyBackingFieldReferenceBuilder.() -> Unit): AstPropertyBackingFieldReference {
    return AstPropertyBackingFieldReferenceBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstPropertyBackingFieldReference.copy(init: AstPropertyBackingFieldReferenceBuilder.() -> Unit = {}): AstPropertyBackingFieldReference {
    val copyBuilder = AstPropertyBackingFieldReferenceBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.property = property
    return copyBuilder.apply(init).build()
}
