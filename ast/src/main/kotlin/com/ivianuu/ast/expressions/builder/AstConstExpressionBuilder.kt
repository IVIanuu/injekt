package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.expressions.AstConst
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.impl.AstConstImpl
import com.ivianuu.ast.types.AstType

fun <T> buildConst(
    type: AstType,
    kind: AstConstKind<T>,
    value: T,
    annotations: MutableList<AstFunctionCall> = mutableListOf(),
): AstConst<T> {
    return AstConstImpl(annotations, type, kind, value)
}
