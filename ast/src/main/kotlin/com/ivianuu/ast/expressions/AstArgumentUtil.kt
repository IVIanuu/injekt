package com.ivianuu.ast.expressions

import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.builder.buildArgumentList
import com.ivianuu.ast.expressions.impl.AstArraySetArgumentList
import com.ivianuu.ast.expressions.impl.AstResolvedArgumentList

fun buildUnaryArgumentList(argument: AstExpression): AstArgumentList = buildArgumentList {
    arguments += argument
}

fun buildBinaryArgumentList(left: AstExpression, right: AstExpression): AstArgumentList =
    buildArgumentList {
        arguments += left
        arguments += right
    }

fun buildArraySetArgumentList(
    rValue: AstExpression,
    indexes: List<AstExpression>
): AstArgumentList =
    AstArraySetArgumentList(rValue, indexes)

fun buildResolvedArgumentList(mapping: Map<AstExpression, AstValueParameter>): AstArgumentList =
    AstResolvedArgumentList(mapping)

object AstEmptyArgumentList : AstAbstractArgumentList() {
    override val arguments: List<AstExpression>
        get() = emptyList()
}
