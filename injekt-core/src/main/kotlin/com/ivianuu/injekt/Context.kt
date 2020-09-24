package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

@Target(AnnotationTarget.CLASS)
annotation class Context

fun <T : Any> rootContext(vararg inputs: Any?): T = injektIntrinsic()

@Reader
fun <T : Any> childContext(vararg inputs: Any?): T = injektIntrinsic()

inline fun <R> Any.runReader(block: () -> R): R = injektIntrinsic()
