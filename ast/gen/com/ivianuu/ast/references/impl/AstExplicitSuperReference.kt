package com.ivianuu.ast.references.impl

import com.ivianuu.ast.references.AstSuperReference
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstExplicitSuperReference(
    override val labelName: String?,
    override var superTypeRef: AstTypeRef,
) : AstSuperReference() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        superTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstExplicitSuperReference {
        superTypeRef = superTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun replaceSuperTypeRef(newSuperTypeRef: AstTypeRef) {
        superTypeRef = newSuperTypeRef
    }
}
