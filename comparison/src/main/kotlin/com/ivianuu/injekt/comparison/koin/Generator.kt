package com.ivianuu.injekt.comparison.koin

import com.ivianuu.injekt.comparison.base.mainstreamKotlinDslGenerator

fun main() {
    println(
        mainstreamKotlinDslGenerator(
            "fun createModule() = module {",
            "factory",
            "get"
        )
    )
}