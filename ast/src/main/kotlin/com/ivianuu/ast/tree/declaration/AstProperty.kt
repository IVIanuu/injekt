package com.ivianuu.ast.tree.declaration

import com.ivianuu.ast.tree.AstExpectActual
import com.ivianuu.ast.tree.AstModality
import com.ivianuu.ast.tree.AstVisibility
import com.ivianuu.ast.tree.expression.AstExpression
import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
import com.ivianuu.ast.tree.visitor.transformSingle
import org.jetbrains.kotlin.name.Name

class AstProperty(
    override var name: Name,
    var type: AstType,
    var kind: Kind = Kind.VAl,
    override var modality: AstModality = AstModality.FINAL,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var expectActual: AstExpectActual? = null,
    var isInline: Boolean = false,
    var isExternal: Boolean = false,
    var dispatchReceiverType: AstType? = null,
    var extensionReceiverType: AstType? = null
) : AstDeclarationBase(),
    AstDeclarationParent,
    AstDeclarationWithName,
    AstDeclarationWithModality,
    AstDeclarationWithVisibility,
    AstDeclarationWithExpectActual,
    AstTypeParameterContainer,
    AstOverridableDeclaration<AstProperty> {

    override val overriddenDeclarations: MutableList<AstProperty> = mutableListOf()

    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()

    var initializer: AstExpression? = null
    var delegate: AstExpression? = null

    var getter: AstFunction? = null
    var setter: AstFunction? = null

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitProperty(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        initializer = initializer?.transformSingle(transformer, data)
        delegate = delegate?.transformSingle(transformer, data)
        getter = getter?.transformSingle(transformer, data)
        setter = setter?.transformSingle(transformer, data)
    }

    enum class Kind {
        VAl, VAR, LATEINIT_VAR, CONST_VAL
    }

}
