package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstExpectActual
import com.ivianuu.injekt.compiler.ast.tree.AstModality
import com.ivianuu.injekt.compiler.ast.tree.AstTarget
import com.ivianuu.injekt.compiler.ast.tree.AstVisibility
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformSingle
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
) : AstDeclarationBase(), AstAnnotationContainer,
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
        typeParameters.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        typeParameters.transformInplace(transformer, data)
        valueParameters.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
    }

    enum class Kind {
        SIMPLE_FUNCTION,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        CONSTRUCTOR
    }

}
