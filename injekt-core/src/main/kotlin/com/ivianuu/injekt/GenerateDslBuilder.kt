package com.ivianuu.injekt

/**
 * Generates a dsl builder function for the annotated behavior
 */
@Target(AnnotationTarget.PROPERTY)
annotation class GenerateDslBuilder(val name: String = "")
