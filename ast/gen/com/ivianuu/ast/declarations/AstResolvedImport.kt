package com.ivianuu.ast.declarations

import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstResolvedImport : AstImport() {
    abstract override val importedFqName: FqName?
    abstract override val isAllUnder: Boolean
    abstract override val aliasName: Name?
    abstract val delegate: AstImport
    abstract val packageFqName: FqName
    abstract val relativeClassName: FqName?
    abstract val resolvedClassId: ClassId?
    abstract val importedName: Name?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitResolvedImport(this, data)
}
