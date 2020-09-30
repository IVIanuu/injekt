package com.ivianuu.injekt

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPEALIAS)
annotation class RootFactory

@Target(AnnotationTarget.TYPEALIAS)
annotation class ChildFactory

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
annotation class Module

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

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Assisted
