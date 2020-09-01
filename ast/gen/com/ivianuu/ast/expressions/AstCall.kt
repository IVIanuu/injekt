package com.ivianuu.ast.expressions

import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstCall : AstStatement {
    override val annotations: List<AstAnnotationCall>
    val argumentList: AstArgumentList

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitCall(this, data)

    fun replaceArgumentList(newArgumentList: AstArgumentList)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstCall
}
