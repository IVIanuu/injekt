package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstCatch : AstPureAbstractElement(), AstElement {
    abstract val parameter: AstValueParameter
    abstract val block: AstBlock

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCatch(this, data)

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstCatch
}
