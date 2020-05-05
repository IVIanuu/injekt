package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

fun <T> createInstance(): T = injektIntrinsic()

fun <T> createImpl(): T = injektIntrinsic()
