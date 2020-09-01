package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformSingle
import org.jetbrains.kotlin.name.Name

class AstValueParameter(
    override var name: Name,
    var type: AstType,
    var isVararg: Boolean = false,
    var inlineHint: InlineHint? = null,
    var defaultValue: AstExpression? = null
) : AstDeclarationBase(), AstDeclarationWithName {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitValueParameter(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        defaultValue?.accept(visitor, data)
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        defaultValue = defaultValue?.transformSingle(transformer, data)
        type = type.transformSingle(transformer, data)
    }

    enum class InlineHint {
        NOINLINE,
        CROSSINLINE
    }

}
