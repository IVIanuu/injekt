package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * A key for a [Binding]
 */
data class Key(
    val type: KClass<*>,
    val qualifier: Qualifier? = null
)