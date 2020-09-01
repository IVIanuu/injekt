package com.ivianuu.ast.tree.declaration

import com.ivianuu.ast.tree.AstElement
import com.ivianuu.ast.tree.AstVariance
import com.ivianuu.ast.tree.type.AstClassifier
import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
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
        annotations.forEach { it.accept(visitor, data) }
        superTypes.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        superTypes.transformInplace(transformer, data)
    }

}

interface AstTypeParameterContainer : AstElement {
    val typeParameters: MutableList<AstTypeParameter>
}
