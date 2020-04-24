package com.ivianuu.injekt

fun interface Provider<T> {
    operator fun invoke(): T
}

fun interface Lazy<T> : Provider<T>
