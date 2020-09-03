package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstFunctionCall
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstNamedDeclaration : AstPureAbstractElement(), AstDeclaration {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract val name: Name

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitNamedDeclaration(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)
}
