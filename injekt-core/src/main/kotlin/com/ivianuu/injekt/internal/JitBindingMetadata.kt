package com.ivianuu.injekt.internal

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class JitBindingMetadata(
    val type: KClass<*>,
    val binding: KClass<*>
)
