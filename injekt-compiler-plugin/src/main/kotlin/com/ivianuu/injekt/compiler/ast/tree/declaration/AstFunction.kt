package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstCallableId
import com.ivianuu.injekt.compiler.ast.tree.AstExpectActual
import com.ivianuu.injekt.compiler.ast.tree.AstModality
import com.ivianuu.injekt.compiler.ast.tree.AstVisibility
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor

abstract class AstFunction : AstDeclarationBase(), AstAnnotationContainer,
    AstDeclarationParent,
    AstDeclarationWithVisibility,
    AstDeclarationWithExpectActual
/*AstTypeParameterContainer*/ {

    abstract var callableId: AstCallableId

    //override var typeParameters: List<AstTypeParameter> = emptyList()
    //var valueParameters: List<AstValueParameter> = emptyList()

    abstract var returnType: AstType

    //var body: AstBody? = null
}

class AstSimpleFunction(
    override var callableId: AstCallableId,
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
) : AstFunction(), AstDeclarationWithModality {
    var overriddenFunctions: List<AstSimpleFunction> = emptyList()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        TODO()
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        TODO()
    }

}

class AstConstructor(
    override var callableId: AstCallableId,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var expectActual: AstExpectActual? = null,
    override var returnType: AstType,
    var isPrimary: Boolean = false
) : AstFunction() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitConstructor(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        TODO()
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        TODO()
    }

}
