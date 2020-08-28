package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstExpectActual
import com.ivianuu.injekt.compiler.ast.tree.AstModality
import com.ivianuu.injekt.compiler.ast.tree.AstVisibility
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import org.jetbrains.kotlin.name.Name

class AstSimpleFunction(
    override var name: Name,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var expectActual: AstExpectActual? = null,
    override var modality: AstModality = AstModality.FINAL,
    override var returnType: AstType,
    var isInline: Boolean = false,
    var isExternal: Boolean = false,
    var isInfix: Boolean = false,
    var isOperator: Boolean = false,
    var isTailrec: Boolean = false,
    var isSuspend: Boolean = false
) : AstFunction(), AstDeclarationWithName, AstDeclarationWithModality {
    var overriddenFunctions: List<AstSimpleFunction> = emptyList()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        typeParameters.transformInplace(transformer, data)
        valueParameters.transformInplace(transformer, data)
    }

}
