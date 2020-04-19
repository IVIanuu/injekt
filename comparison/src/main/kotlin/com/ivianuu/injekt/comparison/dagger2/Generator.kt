package com.ivianuu.injekt.comparison.dagger2

import com.ivianuu.injekt.comparison.fibonacci.FIB_COUNT

fun main() {
    println(
        buildString {
            appendln(
                """
                @Module
                class Dagger2Module {
            """.trimIndent()
            )
            (1..FIB_COUNT).forEach { index ->
                if (index == 1 || index == 2) {
                    appendln("@Provides fun fib$index() = Fib$index()")
                } else {
                    appendln("@Provides fun fib$index(fibM1: Fib${index - 1}, fibM2: Fib${index - 2}) = Fib$index(fibM1, fibM2)")
                }
            }
            append("}")
        }
    )
}