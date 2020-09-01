package com.ivianuu.injekt.compiler.ast.tree.expression

import com.ivianuu.injekt.compiler.ast.tree.type.AstType

interface AstExpression : AstStatement, AstVarargElement {
    var type: AstType
}

abstract class AstExpressionBase : AstExpression {

    override val annotations: MutableList<AstQualifiedAccess> = mutableListOf()

}
