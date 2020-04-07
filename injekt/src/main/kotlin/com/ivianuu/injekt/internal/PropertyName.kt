package com.ivianuu.injekt.internal

fun propertyName(): String = error("Must be compiled with the injekt compiler")

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class PropertyName
