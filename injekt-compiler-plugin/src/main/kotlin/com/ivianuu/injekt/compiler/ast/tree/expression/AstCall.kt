package com.ivianuu.injekt.compiler.ast.tree.expression

import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformSingle

class AstCall(
    override var type: AstType,
    var callee: AstFunction
) : AstExpressionBase() {

    var receiver: AstExpression? = null

    val typeArguments: MutableList<AstType> = mutableListOf()
    val valueArguments: MutableList<AstExpression?> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitCall(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        receiver?.accept(visitor, data)
        typeArguments.forEach { it.accept(visitor, data) }
        valueArguments.forEach { it?.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        receiver = receiver?.transformSingle(transformer, data)
        typeArguments.transformInplace(transformer, data)
        valueArguments.transformInplace(transformer, data)
    }

}
