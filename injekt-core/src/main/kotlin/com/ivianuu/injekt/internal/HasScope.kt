package com.ivianuu.injekt.internal

import kotlin.reflect.KClass

interface HasScope {
    val scope: KClass<*>
}
