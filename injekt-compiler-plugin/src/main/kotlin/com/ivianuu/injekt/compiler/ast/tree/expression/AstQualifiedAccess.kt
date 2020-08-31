package com.ivianuu.injekt.compiler.ast.tree.expression

import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformSingle

class AstQualifiedAccess(
    var callee: AstDeclaration,
    override var type: AstType,
    var safe: Boolean = false
) : AstExpressionBase() {

    val typeArguments: MutableList<AstType> = mutableListOf()
    var dispatchReceiver: AstExpression? = null
    var extensionReceiver: AstExpression? = null
    val valueArguments: MutableList<AstExpression?> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitQualifiedAccess(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        dispatchReceiver?.accept(visitor, data)
        extensionReceiver?.accept(visitor, data)
        typeArguments.forEach { it.accept(visitor, data) }
        valueArguments.forEach { it?.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        dispatchReceiver = dispatchReceiver?.transformSingle(transformer, data)
        extensionReceiver = extensionReceiver?.transformSingle(transformer, data)
        typeArguments.transformInplace(transformer, data)
        valueArguments.transformInplace(transformer, data)
    }

}
