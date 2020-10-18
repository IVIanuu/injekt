package com.ivianuu.injekt

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
annotation class FunBinding

@Target(AnnotationTarget.CLASS)
annotation class ImplBinding(val scopeComponent: KClass<*> = Nothing::class)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class TypeBinding(val scopeComponent: KClass<*> = Nothing::class)
