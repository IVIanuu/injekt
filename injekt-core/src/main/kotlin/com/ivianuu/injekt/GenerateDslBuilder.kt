package com.ivianuu.injekt

/**
 * Generates a dsl builder function for the annotated behavior
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class GenerateDslBuilder(val name: String = "")
