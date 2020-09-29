package com.ivianuu.injekt.merge

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class MergeComponent

@Target(AnnotationTarget.TYPEALIAS)
annotation class MergeFactory(val parent: KClass<*> = Nothing::class)

annotation class GenerateMergeFactories

fun <T> mergeFactory(): T = error("Must be compiled with the Injekt compiler")

@MergeComponent
interface ApplicationComponent
