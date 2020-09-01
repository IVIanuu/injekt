package com.ivianuu.ast.types.impl

import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstResolvedFunctionTypeRef
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle
import org.jetbrains.kotlin.fir.types.ConeKotlinType

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstResolvedFunctionTypeRefImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override val type: ConeKotlinType,
    override val isSuspend: Boolean,
    override val isMarkedNullable: Boolean,
    override var receiverTypeRef: AstTypeRef?,
    override val valueParameters: MutableList<AstValueParameter>,
    override var returnTypeRef: AstTypeRef,
) : AstResolvedFunctionTypeRef() {
    override val delegatedTypeRef: AstTypeRef? get() = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        receiverTypeRef?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstResolvedFunctionTypeRefImpl {
        transformAnnotations(transformer, data)
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstResolvedFunctionTypeRefImpl {
        annotations.transformInplace(transformer, data)
        return this
    }
}
