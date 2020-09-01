package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstJump<E : AstTargetElement> : AstExpression() {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract val target: AstTarget<E>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitJump(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstJump<E>
}
