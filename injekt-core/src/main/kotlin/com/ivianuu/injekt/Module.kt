package com.ivianuu.injekt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Module(val includes: Array<KClass<*>> = [])

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class Provide
