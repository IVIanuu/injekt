package com.ivianuu.ast.references

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstSuperReference : AstReference() {
    abstract val labelName: String?
    abstract val superType: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitSuperReference(this, data)

    abstract fun replaceSuperType(newSuperType: AstType)
}
