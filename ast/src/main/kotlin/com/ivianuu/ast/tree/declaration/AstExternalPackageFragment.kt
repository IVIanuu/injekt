package com.ivianuu.ast.tree.declaration

import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
import org.jetbrains.kotlin.name.FqName

class AstExternalPackageFragment(override var packageFqName: FqName) : AstPackageFragment {

    override val declarations: MutableList<AstDeclaration> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitPackageFragment(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        declarations.transformInplace(transformer, data)
    }

}
