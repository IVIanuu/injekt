package com.ivianuu.ast.types

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstType : AstPureAbstractElement(), AstAnnotationContainer {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract val classifier: AstClassifierSymbol<*>
    abstract val arguments: List<AstTypeProjection>
    abstract val isMarkedNullable: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitType(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract fun replaceClassifier(newClassifier: AstClassifierSymbol<*>)

    abstract fun replaceArguments(newArguments: List<AstTypeProjection>)

    abstract fun replaceIsMarkedNullable(newIsMarkedNullable: Boolean)
}
