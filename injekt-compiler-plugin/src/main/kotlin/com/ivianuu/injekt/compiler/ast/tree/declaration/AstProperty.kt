package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstExpectActual
import com.ivianuu.injekt.compiler.ast.tree.AstModality
import com.ivianuu.injekt.compiler.ast.tree.AstVisibility
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformSingle
import org.jetbrains.kotlin.name.Name

class AstProperty(
    override var name: Name,
    var type: AstType,
    var kind: Kind = Kind.VAl,
    override var modality: AstModality = AstModality.FINAL,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var expectActual: AstExpectActual? = null,
    var isExternal: Boolean = false
) : AstDeclarationBase(), AstAnnotationContainer,
    AstDeclarationParent,
    AstDeclarationWithName,
    AstDeclarationWithModality,
    AstDeclarationWithVisibility,
    AstDeclarationWithExpectActual,
    AstTypeParameterContainer {

    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()

    var overriddenProperties: List<AstProperty> = emptyList()

    var initializer: AstExpression? = null
    var delegate: AstExpression? = null

    var getter: AstSimpleFunction? = null
    var setter: AstSimpleFunction? = null

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitProperty(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
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
