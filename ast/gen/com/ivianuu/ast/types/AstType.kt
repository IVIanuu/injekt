package com.ivianuu.ast.types

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstType : AstPureAbstractElement(), AstAnnotationContainer {
    abstract override val annotations: List<AstFunctionCall>
    abstract val isMarkedNullable: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitType(this, data)
}
