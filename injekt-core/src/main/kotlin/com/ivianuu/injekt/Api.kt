package com.ivianuu.injekt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Component

@Target(AnnotationTarget.CLASS)
annotation class ChildComponent

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Module

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY
)
annotation class Binding(val scopeComponent: KClass<*> = Nothing::class)

@Target(AnnotationTarget.FUNCTION)
annotation class FunBinding

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class MapEntries

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class SetElements

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Assisted
