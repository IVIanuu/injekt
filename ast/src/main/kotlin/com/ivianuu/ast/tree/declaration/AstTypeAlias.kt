package com.ivianuu.ast.tree.declaration

import com.ivianuu.ast.tree.AstExpectActual
import com.ivianuu.ast.tree.AstVisibility
import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
import org.jetbrains.kotlin.name.Name

class AstTypeAlias(
    override var name: Name,
    var type: AstType,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var expectActual: AstExpectActual? = null
) : AstDeclarationBase(), AstDeclarationWithName, AstTypeParameterContainer, AstDeclarationParent,
    AstDeclarationWithVisibility,
    AstDeclarationWithExpectActual {

    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTypeAlias(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
    }

}
