package com.ivianuu.ast.tree.declaration

import com.ivianuu.ast.tree.AstExpectActual
import com.ivianuu.ast.tree.AstModality
import com.ivianuu.ast.tree.AstTarget
import com.ivianuu.ast.tree.AstVisibility
import com.ivianuu.ast.tree.expression.AstBlock
import com.ivianuu.ast.tree.type.AstType
import com.ivianuu.ast.tree.visitor.AstTransformer
import com.ivianuu.ast.tree.visitor.AstVisitor
import com.ivianuu.ast.tree.visitor.transformInplace
import com.ivianuu.ast.tree.visitor.transformSingle
import org.jetbrains.kotlin.name.Name

class AstFunction(
    override var name: Name,
    var kind: Kind,
    var returnType: AstType,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var expectActual: AstExpectActual? = null,
    override var modality: AstModality = AstModality.FINAL,
    var isInline: Boolean = false,
    var isExternal: Boolean = false,
    var isInfix: Boolean = false,
    var isOperator: Boolean = false,
    var isTailrec: Boolean = false,
    var isSuspend: Boolean = false,
    var dispatchReceiverType: AstType? = null,
    var extensionReceiverType: AstType? = null
) : AstDeclarationBase(),
    AstDeclarationParent,
    AstDeclarationWithName,
    AstDeclarationWithVisibility,
    AstDeclarationWithExpectActual,
    AstDeclarationWithModality,
    AstTypeParameterContainer,
    AstTarget,
    AstOverridableDeclaration<AstFunction> {

    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()

    val valueParameters: MutableList<AstValueParameter> = mutableListOf()

    override val overriddenDeclarations: MutableList<AstFunction> = mutableListOf()

    var body: AstBlock? = null

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitFunction(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        dispatchReceiverType?.accept(visitor, data)
        extensionReceiverType?.accept(visitor, data)
        returnType.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        dispatchReceiverType = dispatchReceiverType?.transformSingle(transformer, data)
        extensionReceiverType = extensionReceiverType?.transformSingle(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        typeParameters.transformInplace(transformer, data)
        valueParameters.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
    }

    enum class Kind {
        SIMPLE_FUNCTION,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        CONSTRUCTOR,
        ANONYMOUS_FUNCTION
    }

}
