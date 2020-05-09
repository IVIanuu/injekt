package com.ivianuu.injekt

@Target(AnnotationTarget.CLASS)
annotation class EntryPoint

inline fun <reified T> entryPointOf(component: Any): T = component as T
