package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstSuperReference
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstSuperReferenceImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstSuperReferenceBuilder : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    var labelName: String? = null
    lateinit var superType: AstType

    override fun build(): AstSuperReference {
        return AstSuperReferenceImpl(
            annotations,
            type,
            labelName,
            superType,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSuperReference(init: AstSuperReferenceBuilder.() -> Unit): AstSuperReference {
    return AstSuperReferenceBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstSuperReference.copy(init: AstSuperReferenceBuilder.() -> Unit = {}): AstSuperReference {
    val copyBuilder = AstSuperReferenceBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.labelName = labelName
    copyBuilder.superType = superType
    return copyBuilder.apply(init).build()
}
