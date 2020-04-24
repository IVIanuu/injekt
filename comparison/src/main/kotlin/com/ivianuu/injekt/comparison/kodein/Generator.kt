package com.ivianuu.injekt.comparison.kodein

import com.ivianuu.injekt.comparison.fibonacci.FIB_COUNT

fun main() {
    println(
        buildString {
            appendln("fun createModule() = Kodein.Module(\"fib\") {")
            (1..FIB_COUNT).forEach { index ->
                if (index == 1 || index == 2) {
                    appendln("bind<Fib${index}>() with provider { Fib${index}() }")
                } else {
                    appendln(
                        """
                            bind<Fib$index>() with provider {
                                Fib$index(instance(),instance())
                                }
                        """.trimIndent()
                    )
                }
            }
            append("}")
        }
    )
}