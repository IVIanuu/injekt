package com.ivianuu.injekt.comparison.dagger2

fun main() {
    println(
        buildString {
            (5..100).forEach { index ->
                appendln(
                    """
                        @Provides
                        fun fib$index(fib2: Fib${index - 1}, fib1: Fib${index - 2}) = Fib$index(fib2, fib1)
                    """.trimIndent()
                )
            }
        }
    )
}