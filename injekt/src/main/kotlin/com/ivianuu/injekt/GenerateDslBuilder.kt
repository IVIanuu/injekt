package com.ivianuu.injekt

/**
 * Generates a dsl builder function for the annotated behavior
 *
 * @see Factory
 * @see Single
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class GenerateDslBuilder
