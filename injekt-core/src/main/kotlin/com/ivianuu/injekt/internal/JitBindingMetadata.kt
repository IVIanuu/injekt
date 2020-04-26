package com.ivianuu.injekt.internal

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class JitBindingMetadata(
    val type: KClass<*>,
    val scope: KClass<*>,
    val binding: KClass<*>
)
