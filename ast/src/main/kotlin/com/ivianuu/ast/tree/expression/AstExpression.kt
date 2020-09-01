package com.ivianuu.ast.tree.expression

import com.ivianuu.ast.tree.type.AstType

interface AstExpression : AstStatement, AstVarargElement {
    var type: AstType
}

abstract class AstExpressionBase : AstExpression {

    override val annotations: MutableList<AstQualifiedAccess> = mutableListOf()

}
