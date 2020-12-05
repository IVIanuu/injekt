package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.ApplicationComponent

@Binding(ApplicationComponent::class)
class ErrorCollector {

    private val errors = mutableListOf<Throwable>()

    fun add(message: String): Nothing {
        add(RuntimeException(message))
    }

    fun add(throwable: Throwable): Nothing {
        errors += throwable
        throw ExitGenerationException()
    }

    fun report() {
        if (errors.isNotEmpty()) {
            val combined = RuntimeException("Failed to process all declarations.")
            errors.forEach { combined.addSuppressed(it) }
            throw combined
        }
    }

}
