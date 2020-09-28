package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Context
import kotlin.reflect.KClass

annotation class EntryPoint(
    val targetContext: KClass<out Context>,
    val entryPoint: KClass<*>
)
