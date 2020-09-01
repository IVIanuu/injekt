package com.ivianuu.ast.tree.declaration

import com.ivianuu.ast.tree.AstElement
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
import org.jetbrains.kotlin.name.Name

class AstModuleFragment(val name: Name) : AstElement {

    val files = mutableListOf<AstFile>()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitModuleFragment(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        files.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        files.transformInplace(transformer, data)
    }

}
