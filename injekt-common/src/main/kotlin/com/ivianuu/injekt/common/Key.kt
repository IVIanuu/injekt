package com.ivianuu.injekt.common

inline class Key<out T>(val value: String) {
    override fun toString(): String = value
}

fun <@ForKey T> keyOf(): Key<T> = error("Intrinsic")

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class ForKey
