package com.ivianuu.injekt.generator

fun main() {
    println(
        """
        interface AssistedParameters {
            ${buildString { (1..MAX_PARAMETERS).forEach { appendln("    operator fun <T> component$it(): T") } }}
        }

    """.trimIndent()
    )
}