package com.ivianuu.ast.references.impl

import com.ivianuu.ast.references.AstSuperReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstExplicitSuperReference(
    override val labelName: String?,
    override var superType: AstType,
) : AstSuperReference() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        superType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstExplicitSuperReference {
        superType = superType.transformSingle(transformer, data)
        return this
    }

    override fun replaceSuperType(newSuperType: AstType) {
        superType = newSuperType
    }
}
