package com.ivianuu.injekt.compiler

class ExitGenerationException : RuntimeException()

inline fun runExitCatching(block: () -> Unit) {
    try {
        block()
    } catch (e: ExitGenerationException) {
    }
}
