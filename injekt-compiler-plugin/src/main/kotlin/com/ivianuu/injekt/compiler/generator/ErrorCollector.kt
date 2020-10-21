package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding

@Binding(GenerationComponent::class)
class ErrorCollector {

    private val errors = mutableListOf<Throwable>()

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
