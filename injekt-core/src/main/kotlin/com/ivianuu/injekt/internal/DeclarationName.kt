package com.ivianuu.injekt.internal

/** Only used in codegen */
fun declarationName(): String = error("Must be compiled with the injekt compiler")

/** Only used in codegen */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class DeclarationName
