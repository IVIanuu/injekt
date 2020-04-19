package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.comparison.base.mainstreamKotlinDslGenerator

fun main() {
    println(
        mainstreamKotlinDslGenerator(
            """
                @Module
                fun fibonacci() {
            """.trimIndent(),
            "factory",
            "get"
        )
    )
}
