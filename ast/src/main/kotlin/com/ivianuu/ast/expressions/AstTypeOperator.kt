package com.ivianuu.ast.expressions

enum class AstTypeOperator(val keyword: String) {
    AS("as"),
    SAFE_AS("as?"),
    IS("is"),
    IS_NOT("!is")
}
