package com.ivianuu.ast.types.impl

import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstSimpleTypeImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override var isMarkedNullable: Boolean,
    override var classifier: AstClassifierSymbol<*>,
    override val arguments: MutableList<AstTypeProjection>,
) : AstSimpleType() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstSimpleTypeImpl {
        annotations.transformInplace(transformer, data)
        arguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceIsMarkedNullable(newIsMarkedNullable: Boolean) {
        isMarkedNullable = newIsMarkedNullable
    }

    override fun replaceClassifier(newClassifier: AstClassifierSymbol<*>) {
        classifier = newClassifier
    }

    override fun replaceArguments(newArguments: List<AstTypeProjection>) {
        arguments.clear()
        arguments.addAll(newArguments)
    }
}
