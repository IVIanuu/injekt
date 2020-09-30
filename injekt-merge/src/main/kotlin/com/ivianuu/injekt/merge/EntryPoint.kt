package com.ivianuu.injekt.merge

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class EntryPoint(val installIn: KClass<*>)

@Suppress("UNCHECKED_CAST")
fun <T> Any.entryPoint(): T = this as T
