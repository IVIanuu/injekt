package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstImport : AstPureAbstractElement(), AstElement {
    abstract val importedFqName: FqName?
    abstract val isAllUnder: Boolean
    abstract val aliasName: Name?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitImport(this, data)
}
