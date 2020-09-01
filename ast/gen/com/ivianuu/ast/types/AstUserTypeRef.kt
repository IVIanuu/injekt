package com.ivianuu.ast.types

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstUserTypeRef : AstPureAbstractElement(), AstTypeRefWithNullability {
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val isMarkedNullable: Boolean
    abstract val qualifier: List<AstQualifierPart>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitUserTypeRef(this, data)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstUserTypeRef
}
