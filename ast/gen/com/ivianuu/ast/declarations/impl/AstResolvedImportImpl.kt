package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstImport
import com.ivianuu.ast.declarations.AstResolvedImport
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstResolvedImportImpl(
    override var delegate: AstImport,
    override val packageFqName: FqName,
    override val relativeClassName: FqName?,
) : AstResolvedImport() {
    override val importedFqName: FqName? get() = delegate.importedFqName
    override val isAllUnder: Boolean get() = delegate.isAllUnder
    override val aliasName: Name? get() = delegate.aliasName
    override val resolvedClassId: ClassId?
        get() = relativeClassName?.let {
            ClassId(
                packageFqName,
                it,
                false
            )
        }
    override val importedName: Name? get() = importedFqName?.shortName()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstResolvedImportImpl {
        return this
    }
}
