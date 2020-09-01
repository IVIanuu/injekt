package com.ivianuu.ast.builder

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.impl.AstLabelImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstLabelBuilder {
    lateinit var name: String

    fun build(): AstLabel {
        return AstLabelImpl(
            name,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildLabel(init: AstLabelBuilder.() -> Unit): AstLabel {
    return AstLabelBuilder().apply(init).build()
}
