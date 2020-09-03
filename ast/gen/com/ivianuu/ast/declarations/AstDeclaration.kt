package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstDeclaration : AstStatement, AstAnnotationContainer {
    override val annotations: List<AstFunctionCall>
    val origin: AstDeclarationOrigin
    val attributes: AstDeclarationAttributes

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDeclaration(this, data)

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)
}
