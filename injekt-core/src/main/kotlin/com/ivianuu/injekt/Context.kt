package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

interface Context

fun rootContext(vararg inputs: Any?, name: KClass<*> = Nothing::class): Context = injektIntrinsic()

@Reader
fun childContext(vararg inputs: Any?, name: KClass<*> = Nothing::class): Context = injektIntrinsic()

inline fun <R> Context.runReader(block: @Reader () -> R): R = injektIntrinsic()
