package com.ivianuu.injekt

import com.ivianuu.injekt.internal._injektIntrinsic

@Target(AnnotationTarget.CLASS)
annotation class Context

fun <T> rootContext(vararg inputs: Any?): T = _injektIntrinsic()

@Reader
fun <T> childContext(vararg inputs: Any?): T = _injektIntrinsic()

inline fun <R> Any.runReader(block: @Reader () -> R): R = _injektIntrinsic()
