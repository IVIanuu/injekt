package com.ivianuu.ast.types

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstComposedSuperTypeRef : AstPureAbstractElement(), AstTypeRef {
    abstract override val annotations: List<AstAnnotationCall>
    abstract val superTypeRefs: List<AstResolvedTypeRef>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitComposedSuperTypeRef(this, data)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstComposedSuperTypeRef
}
