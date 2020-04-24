package com.ivianuu.injekt.comparison.base

import com.ivianuu.injekt.comparison.fibonacci.FIB_COUNT

fun mainstreamKotlinDslGenerator(
    header: String,
    bindKeyword: String,
    getKeyword: String
): String = buildString {
    appendln(header)
    (1..FIB_COUNT).forEach { index ->
        if (index == 1 || index == 2) {
            appendln("$bindKeyword { Fib$index() }")
        } else {
            appendln("$bindKeyword { Fib$index($getKeyword(), $getKeyword()) }")
        }
    }
    append("}")
}