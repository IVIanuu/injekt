package com.ivianuu.ast.types.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTypeImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var classifier: AstClassifierSymbol<*>,
    override val arguments: MutableList<AstTypeProjection>,
    override var isMarkedNullable: Boolean,
) : AstType() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeImpl {
        annotations.transformInplace(transformer, data)
        arguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceClassifier(newClassifier: AstClassifierSymbol<*>) {
        classifier = newClassifier
    }

    override fun replaceArguments(newArguments: List<AstTypeProjection>) {
        arguments.clear()
        arguments.addAll(newArguments)
    }

    override fun replaceIsMarkedNullable(newIsMarkedNullable: Boolean) {
        isMarkedNullable = newIsMarkedNullable
    }
}
