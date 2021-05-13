package com.ivianuu.injekt.common

@Target(AnnotationTarget.CLASS)
annotation class Extension

internal annotation class SyntheticExtensionCallable(val value: String)
