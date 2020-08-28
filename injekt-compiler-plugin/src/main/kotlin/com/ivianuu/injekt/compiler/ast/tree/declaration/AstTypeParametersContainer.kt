package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstVariance
import com.ivianuu.injekt.compiler.ast.tree.type.AstClassifier
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import org.jetbrains.kotlin.name.Name

class AstTypeParameter(
    override var name: Name,
    var isReified: Boolean = false,
    var variance: AstVariance? = null
) : AstDeclarationBase(), AstClassifier, AstDeclarationWithName {

    val superTypes: MutableList<AstType> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTypeParameter(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        val prop: String
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
    }

}
