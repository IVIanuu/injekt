package com.ivianuu.injekt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
annotation class Module(val installIn: KClass<*> = Nothing::class)
