package com.ivianuu.injekt.internal

fun declarationName(): String = error("Must be compiled with the injekt compiler")

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class DeclarationName
