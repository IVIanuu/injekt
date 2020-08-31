package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformSingle

class AstAnonymousInitializer : AstDeclarationBase() {

    lateinit var body: AstBlock

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitAnonymousInitializer(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        body = body.transformSingle(transformer, data)
    }

}
