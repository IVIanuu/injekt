package com.ivianuu.injekt

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private val classNames: MutableMap<KClass<*>, String> = ConcurrentHashMap()

internal fun KClass<*>.getFullName() = classNames[this] ?: saveFullName()

private fun KClass<*>.saveFullName(): String {
    val name = this.java.canonicalName
    classNames[this] = name
    return name
}

internal fun Component.nameString() = if (name != null) {
    "$name "
} else {
    ""
}

internal fun Module.nameString() = if (name != null) {
    "$name "
} else {
    ""
}

fun measureDurationOnly(code: () -> Unit): Double {
    val start = System.nanoTime()
    code()
    return (System.nanoTime() - start) / 1000000.0
}

fun <T> measureDuration(code: () -> T): Pair<T, Double> {
    val start = System.nanoTime()
    val result = code()
    val duration = (System.nanoTime() - start) / 1000000.0
    return Pair(result, duration)
}