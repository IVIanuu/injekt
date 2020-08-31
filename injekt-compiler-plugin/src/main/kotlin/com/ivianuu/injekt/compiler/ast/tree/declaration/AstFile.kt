package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class AstFile(
    override var packageFqName: FqName,
    var name: Name
) : AstElement, AstPackageFragment, AstAnnotationContainer {

    override val annotations: MutableList<AstQualifiedAccess> = mutableListOf()
    override val declarations: MutableList<AstDeclaration> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transform(
        transformer: AstTransformer<D>,
        data: D
    ) = transformer.visitFile(this, data)

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        declarations.transformInplace(transformer, data)
    }

}
