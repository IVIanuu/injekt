package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class AstEmptyExpressionBlock : AstBlock() {
    override val annotations: List<AstAnnotationCall> get() = emptyList()
    override val statements: List<AstStatement> get() = emptyList()
    override var type: AstType = AstImplicitTypeImpl()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstEmptyExpressionBlock {
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstEmptyExpressionBlock {
        return this
    }

    override fun <D> transformStatements(transformer: AstTransformer<D>, data: D): AstEmptyExpressionBlock {
        return this
    }

    override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstEmptyExpressionBlock {
        type = type.transformSingle(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
