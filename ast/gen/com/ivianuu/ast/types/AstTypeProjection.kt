package com.ivianuu.ast.types

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTypeProjection : AstPureAbstractElement(), AstElement {
    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTypeProjection(this, data)
}
