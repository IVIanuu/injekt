package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstPackageFragment : AstDeclarationContainer {
    override val context: AstContext
    override val declarations: List<AstDeclaration>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitPackageFragment(this, data)

    override fun replaceDeclarations(newDeclarations: List<AstDeclaration>)
}
