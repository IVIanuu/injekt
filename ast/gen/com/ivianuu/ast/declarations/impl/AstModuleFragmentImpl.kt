package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.AstModuleFragment
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstModuleFragmentImpl(
    override var name: Name,
    override val files: MutableList<AstFile>,
) : AstModuleFragment() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        files.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstModuleFragmentImpl {
        files.transformInplace(transformer, data)
        return this
    }

    override fun replaceFiles(newFiles: List<AstFile>) {
        files.clear()
        files.addAll(newFiles)
    }
}
