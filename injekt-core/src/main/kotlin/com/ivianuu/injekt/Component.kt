package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

interface Component {
    fun <T> get(key: Int): T
}

fun <T> component(block: @Module () -> Unit = {}): T = injektIntrinsic()

@Module
fun <T> parent(dependency: T): Unit = injektIntrinsic()

@Module
fun <T> scope(): Unit = injektIntrinsic()
