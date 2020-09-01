package com.ivianuu.ast.expressions.impl

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

internal class AstBlockImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override val statements: MutableList<AstStatement>,
) : AstBlock() {
    override var type: AstType = AstImplicitTypeImpl()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        statements.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstBlockImpl {
        transformStatements(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstBlockImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatements(transformer: AstTransformer<D>, data: D): AstBlockImpl {
        statements.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstBlockImpl {
        transformAnnotations(transformer, data)
        type = type.transformSingle(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
