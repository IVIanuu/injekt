package com.ivianuu.injekt.comparison.katana

import com.ivianuu.injekt.comparison.base.mainstreamKotlinDslGenerator

fun main() {
    println(
        mainstreamKotlinDslGenerator(
            "fun createModule() = Module(\"katanaKotlinModule\") {",
            "factory",
            "get"
        )
    )
}