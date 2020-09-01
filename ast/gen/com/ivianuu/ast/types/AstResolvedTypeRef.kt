package com.ivianuu.ast.types

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.fir.types.ConeKotlinType

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstResolvedTypeRef : AstPureAbstractElement(), AstTypeRef {
    abstract override val annotations: List<AstAnnotationCall>
    abstract val type: ConeKotlinType
    abstract val delegatedTypeRef: AstTypeRef?
    abstract val isSuspend: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitResolvedTypeRef(this, data)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstResolvedTypeRef
}
