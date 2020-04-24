package com.ivianuu.injekt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Component(
    val dependencies: Array<KClass<*>> = [],
    val modules: Array<KClass<*>> = []
) {
    @Target(AnnotationTarget.CLASS)
    annotation class Factory
}

inline fun <reified T> componentFactory(): T = error("Implemented as an intrinsic")
