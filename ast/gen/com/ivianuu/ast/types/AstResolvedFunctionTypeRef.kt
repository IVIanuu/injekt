package com.ivianuu.ast.types

import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstResolvedFunctionTypeRef : AstResolvedTypeRef(), AstFunctionTypeRef {
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val type: ConeKotlinType
    abstract override val delegatedTypeRef: AstTypeRef?
    abstract override val isSuspend: Boolean
    abstract override val isMarkedNullable: Boolean
    abstract override val receiverTypeRef: AstTypeRef?
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val returnTypeRef: AstTypeRef

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitResolvedFunctionTypeRef(this, data)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstResolvedFunctionTypeRef
}
