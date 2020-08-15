package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

@Target(AnnotationTarget.CLASS)
annotation class Component

fun <T> component(
    vararg inputs: Any?
): T = injektIntrinsic()

@Reader
fun <T> childComponent(
    vararg inputs: Any?
): T = injektIntrinsic()

fun <R> Any.runReader(
    block: @Reader () -> R
): R = injektIntrinsic()
