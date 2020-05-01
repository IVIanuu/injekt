package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

fun <T> createInstance(block: @Module () -> Unit = {}): T = injektIntrinsic()

fun <T> createImplementation(block: @Module () -> Unit = {}): T = injektIntrinsic()
