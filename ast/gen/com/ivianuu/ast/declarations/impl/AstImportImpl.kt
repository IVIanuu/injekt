package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstImport
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstImportImpl(
    override val importedFqName: FqName?,
    override val isAllUnder: Boolean,
    override val aliasName: Name?,
) : AstImport() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstImportImpl {
        return this
    }
}
