package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

@Module
fun <T> parent(): Unit = injektIntrinsic()
