package com.ivianuu.injekt.comparison.winter

import com.ivianuu.injekt.comparison.base.mainstreamKotlinDslGenerator

fun main() {
    println(
        mainstreamKotlinDslGenerator(
            "fun Component.Builder.fib() {",
            "prototype",
            "instance"
        )
    )
}
