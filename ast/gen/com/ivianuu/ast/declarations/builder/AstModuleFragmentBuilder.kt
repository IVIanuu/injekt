package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.AstModuleFragment
import com.ivianuu.ast.declarations.impl.AstModuleFragmentImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstModuleFragmentBuilder(override val context: AstContext) : AstBuilder {
    lateinit var name: Name
    val files: MutableList<AstFile> = mutableListOf()

    fun build(): AstModuleFragment {
        return AstModuleFragmentImpl(
            context,
            name,
            files,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildModuleFragment(init: AstModuleFragmentBuilder.() -> Unit): AstModuleFragment {
    return AstModuleFragmentBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstModuleFragment.copy(init: AstModuleFragmentBuilder.() -> Unit = {}): AstModuleFragment {
    val copyBuilder = AstModuleFragmentBuilder(context)
    copyBuilder.name = name
    copyBuilder.files.addAll(files)
    return copyBuilder.apply(init).build()
}
