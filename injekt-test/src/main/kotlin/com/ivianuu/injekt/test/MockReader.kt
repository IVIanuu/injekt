package com.ivianuu.injekt.test

import com.ivianuu.injekt.internal.injektIntrinsic

fun <T : Function<R>, R> mockReader(reader: T, mock: T): T = injektIntrinsic()
