package com.ivianuu.injekt.test

import com.ivianuu.injekt.internal._injektIntrinsic

fun <T : Function<R>, R> mockReader(reader: T, mock: T): T = _injektIntrinsic()
