package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstDeclarationContainer : AstElement {
    val declarations: List<AstDeclaration>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDeclarationContainer(this, data)

    fun replaceDeclarations(newDeclarations: List<AstDeclaration>)
}
