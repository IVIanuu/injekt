package com.ivianuu.injekt.comparison.fibonacci

const val FIB_COUNT = 400

fun main() {
    println(
        buildString {
            (1..FIB_COUNT).forEach { index ->
                if (index == 1 || index == 2) {
                    appendln(
                        """
                        @Factory
                        class Fib$index @Inject constructor()
                        """.trimIndent()
                    )
                } else {
                    appendln(
                        """
                        @Factory
                        class Fib$index @Inject constructor(
                            val fibM1: Fib${index - 1},
                            val fibM2: Fib${index - 2}
                            )""".trimIndent()
                    )
                }

            }
        }
    )
}
