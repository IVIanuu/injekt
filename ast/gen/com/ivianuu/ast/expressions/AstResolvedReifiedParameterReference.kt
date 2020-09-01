package com.ivianuu.ast.expressions

import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstResolvedReifiedParameterReference : AstExpression() {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract val symbol: AstTypeParameterSymbol

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitResolvedReifiedParameterReference(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstResolvedReifiedParameterReference
}
