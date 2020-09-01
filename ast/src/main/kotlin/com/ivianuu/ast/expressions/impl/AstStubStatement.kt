package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

object AstStubStatement : AstPureAbstractElement(), AstStatement {
    override val annotations: List<AstAnnotationCall>
        get() = emptyList()

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstStatement {
        return this
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElement {
        return this
    }
}