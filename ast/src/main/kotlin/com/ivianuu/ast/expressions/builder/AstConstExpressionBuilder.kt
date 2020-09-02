package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstConstExpression
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.impl.AstConstExpressionImpl
import com.ivianuu.ast.types.AstType

fun <T> buildConstExpression(
    type: AstType,
    kind: AstConstKind<T>,
    value: T,
    annotations: MutableList<AstCall> = mutableListOf(),
): AstConstExpression<T> {
    return AstConstExpressionImpl(type, annotations, kind, value)
}