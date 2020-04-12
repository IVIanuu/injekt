package com.ivianuu.injekt

/**
 * Generates dsl builder function for the annotated behavior
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class GenerateDsl(
    val generateBuilder: Boolean = true,
    val builderName: String = "",
    val generateDelegate: Boolean = false,
    val delegateName: String = ""
)
