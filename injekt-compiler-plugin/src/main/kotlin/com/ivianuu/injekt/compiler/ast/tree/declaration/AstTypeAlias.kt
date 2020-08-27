package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor

class AstTypeAlias : AstDeclarationBase() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R {
        TODO()
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        TODO()
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        TODO()
    }

}
