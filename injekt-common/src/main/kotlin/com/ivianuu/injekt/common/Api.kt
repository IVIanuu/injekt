package com.ivianuu.injekt

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
annotation class FunBinding

@Target(AnnotationTarget.CLASS)
annotation class ImplBinding(val scopeComponent: KClass<*> = Nothing::class)
