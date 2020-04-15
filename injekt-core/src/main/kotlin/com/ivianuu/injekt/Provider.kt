package com.ivianuu.injekt

fun interface Provider<T> {
    operator fun invoke(): T
}