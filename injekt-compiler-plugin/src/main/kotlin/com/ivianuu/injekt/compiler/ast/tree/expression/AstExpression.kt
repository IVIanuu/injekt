package com.ivianuu.injekt.compiler.ast.tree.expression

import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformResult
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer

interface AstExpression : AstStatement

abstract class AstExpressionBase : AstExpression {

    abstract var type: AstType
    override val annotations: MutableList<AstCall> = mutableListOf()

    override fun <D> transform(
        transformer: AstTransformer<D>,
        data: D
    ): AstTransformResult<AstStatement> = transformer.visitExpression(this, data)

}
