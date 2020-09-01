package com.ivianuu.ast.references

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstReference : AstPureAbstractElement(), AstElement {
    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitReference(this, data)
}
