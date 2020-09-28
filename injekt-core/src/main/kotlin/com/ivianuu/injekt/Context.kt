package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

interface Context

fun <T : Context> rootContext(vararg inputs: Any?): T = injektIntrinsic()

@Reader
fun <T : Context> childContext(vararg inputs: Any?): T = injektIntrinsic()
