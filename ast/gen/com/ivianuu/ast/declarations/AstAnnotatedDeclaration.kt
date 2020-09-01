package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstAnnotatedDeclaration : AstDeclaration, AstAnnotationContainer {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstAnnotationCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitAnnotatedDeclaration(this, data)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstAnnotatedDeclaration
}
