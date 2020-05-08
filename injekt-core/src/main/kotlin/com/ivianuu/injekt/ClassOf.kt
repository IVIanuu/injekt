package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

fun <T : Any> classOf(): KClass<T> = injektIntrinsic()
