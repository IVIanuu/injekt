package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

@Target(AnnotationTarget.CLASS)
annotation class Context

fun <T> rootContext(vararg inputs: Any?): T = injektIntrinsic()

@Reader
fun <T> childContext(vararg inputs: Any?): T = injektIntrinsic()

inline fun <R> Any.runReader(block: @Reader () -> R): R = injektIntrinsic()
