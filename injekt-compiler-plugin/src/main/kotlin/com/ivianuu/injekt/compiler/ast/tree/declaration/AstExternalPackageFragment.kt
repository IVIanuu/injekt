package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformResult
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import org.jetbrains.kotlin.name.FqName

class AstExternalPackageFragment(override var packageFqName: FqName) : AstPackageFragment {
    override val declarations: MutableList<AstDeclaration> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitPackageFragment(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transform(
        transformer: AstTransformer<D>,
        data: D
    ): AstTransformResult<AstPackageFragment> =
        accept(transformer, data) as AstTransformResult<AstPackageFragment>

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        declarations.transformInplace(transformer, data)
    }

}
