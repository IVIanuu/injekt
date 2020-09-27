package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

interface Context

inline fun <R> Context.runReader(block: () -> R): R = injektIntrinsic()

fun <T : Context> rootContext(vararg inputs: Any?): T = injektIntrinsic()

@Reader
fun <T : Context> childContext(vararg inputs: Any?): T = injektIntrinsic()
