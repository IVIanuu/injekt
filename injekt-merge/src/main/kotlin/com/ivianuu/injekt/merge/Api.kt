package com.ivianuu.injekt.merge

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class MergeComponent

@Target(AnnotationTarget.CLASS)
annotation class MergeChildComponent

@Target(AnnotationTarget.CLASS)
annotation class MergeInto(val component: KClass<*>)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class GenerateMergeComponents

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class BindingComponent(val component: KClass<*>)

fun <T> Any.mergeComponent(): T = this as T
