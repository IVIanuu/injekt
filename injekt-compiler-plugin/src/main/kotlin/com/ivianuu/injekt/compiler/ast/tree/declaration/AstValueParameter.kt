package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformSingle
import org.jetbrains.kotlin.name.Name

class AstValueParameter(
    var name: Name,
    var type: AstType,
    var isVarArg: Boolean?,
    var inlineHint: InlineHint?,
    var defaultValue: AstExpression?
) : AstDeclarationBase() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitValueParameter(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        defaultValue = defaultValue?.transformSingle(transformer, data)
    }

    enum class InlineHint {
        NOINLINE,
        CROSSINLINE
    }

}
