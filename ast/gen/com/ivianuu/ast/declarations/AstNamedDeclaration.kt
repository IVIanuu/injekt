package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.expressions.AstFunctionCall
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstNamedDeclaration : AstDeclaration {
    override val context: AstContext
    override val annotations: List<AstFunctionCall>
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    val name: Name

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitNamedDeclaration(this, data)

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    override fun replaceOrigin(newOrigin: AstDeclarationOrigin)

    override fun replaceAttributes(newAttributes: AstDeclarationAttributes)
}
