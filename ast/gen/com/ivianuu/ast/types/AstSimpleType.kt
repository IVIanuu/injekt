package com.ivianuu.ast.types

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstSimpleType : AstPureAbstractElement(), AstType {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val isMarkedNullable: Boolean
    abstract val classifier: AstClassifierSymbol<*>
    abstract val arguments: List<AstTypeProjection>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitSimpleType(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceIsMarkedNullable(newIsMarkedNullable: Boolean)

    abstract fun replaceClassifier(newClassifier: AstClassifierSymbol<*>)

    abstract fun replaceArguments(newArguments: List<AstTypeProjection>)
}
