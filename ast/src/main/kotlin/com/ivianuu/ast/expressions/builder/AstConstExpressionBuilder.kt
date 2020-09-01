package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstConstExpression
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.impl.AstConstExpressionImpl

fun <T> buildConstExpression(
    kind: AstConstKind<T>,
    value: T,
    annotations: MutableList<AstAnnotationCall> = mutableListOf(),
): AstConstExpression<T> {
    return AstConstExpressionImpl(annotations, kind, value)
}