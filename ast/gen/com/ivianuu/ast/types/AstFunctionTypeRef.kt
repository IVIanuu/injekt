package com.ivianuu.ast.types

import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstFunctionTypeRef : AstTypeRefWithNullability {
    override val annotations: List<AstAnnotationCall>
    override val isMarkedNullable: Boolean
    val receiverTypeRef: AstTypeRef?
    val valueParameters: List<AstValueParameter>
    val returnTypeRef: AstTypeRef
    val isSuspend: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitFunctionTypeRef(this, data)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstFunctionTypeRef
}
