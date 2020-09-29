package com.ivianuu.injekt

import kotlin.reflect.KClass

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY
)
annotation class Given(val scopeComponent: KClass<*> = Nothing::class)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class GivenMapEntries

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class GivenSetElements
