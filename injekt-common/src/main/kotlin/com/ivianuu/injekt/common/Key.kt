package com.ivianuu.injekt.common

data class Key<out T>(val value: String)

fun <@ForKey T> keyOf(): Key<T> = error("Intrinsic")

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class ForKey
