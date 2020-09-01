package com.ivianuu.ast.types.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstSimpleTypeImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override val isMarkedNullable: Boolean,
    override val classifier: AstClassifierSymbol<*>,
    override val arguments: MutableList<AstTypeProjection>,
) : AstSimpleType() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstSimpleTypeImpl {
        transformAnnotations(transformer, data)
        arguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstSimpleTypeImpl {
        annotations.transformInplace(transformer, data)
        return this
    }
}
