package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

fun <T> T.inject(component: Any): Unit = injektIntrinsic()
