package com.ivianuu.ast.types.impl

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstFunctionTypeRef
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstFunctionTypeRefImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override val isMarkedNullable: Boolean,
    override var receiverTypeRef: AstTypeRef?,
    override val valueParameters: MutableList<AstValueParameter>,
    override var returnTypeRef: AstTypeRef,
    override val isSuspend: Boolean,
) : AstPureAbstractElement(), AstFunctionTypeRef {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        receiverTypeRef?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstFunctionTypeRefImpl {
        transformAnnotations(transformer, data)
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstFunctionTypeRefImpl {
        annotations.transformInplace(transformer, data)
        return this
    }
}
