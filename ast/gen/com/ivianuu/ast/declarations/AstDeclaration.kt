package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstDeclaration : AstElement {
    val origin: AstDeclarationOrigin
    val attributes: AstDeclarationAttributes

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDeclaration(this, data)
}
