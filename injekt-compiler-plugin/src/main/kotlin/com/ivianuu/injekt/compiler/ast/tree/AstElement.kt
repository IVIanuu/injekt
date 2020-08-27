package com.ivianuu.injekt.compiler.ast.tree

import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformResult
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformerVoid
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitorVoid

interface AstElement {

    fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R
    fun accept(visitor: AstVisitorVoid) = accept(visitor, null)

    fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D)
    fun acceptChildren(visitor: AstVisitorVoid) = acceptChildren(visitor, null)

    fun <D> transform(transformer: AstTransformer<D>, data: D): AstTransformResult<AstElement>
    fun transform(transformer: AstTransformerVoid) =
        transform(transformer, null)

    fun <D> transformChildren(transformer: AstTransformer<D>, data: D)
    fun transformChildren(transformer: AstTransformerVoid) =
        transformChildren(transformer, null)

}
