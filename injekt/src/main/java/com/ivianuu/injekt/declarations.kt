package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Binds this [Declaration] to [types]
 */
infix fun Declaration<*>.binds(types: Array<KClass<*>>) = apply {
    types.forEach { bind(it) }
}