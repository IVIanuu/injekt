package com.ivianuu.injekt.internal

@Target(AnnotationTarget.CLASS)
annotation class ProviderMetadata(
    val key: String,
    val isSingle: Boolean
)
