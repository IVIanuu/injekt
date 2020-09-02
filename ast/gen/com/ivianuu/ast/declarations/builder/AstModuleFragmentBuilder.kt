package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.AstModuleFragment
import com.ivianuu.ast.declarations.impl.AstModuleFragmentImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstModuleFragmentBuilder {
    lateinit var name: String
    val files: MutableList<AstFile> = mutableListOf()

    fun build(): AstModuleFragment {
        return AstModuleFragmentImpl(
            name,
            files,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildModuleFragment(init: AstModuleFragmentBuilder.() -> Unit): AstModuleFragment {
    return AstModuleFragmentBuilder().apply(init).build()
}
