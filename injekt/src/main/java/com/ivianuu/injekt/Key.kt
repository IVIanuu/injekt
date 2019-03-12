package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * A key for a [Binding]
 */
data class Key(
    val type: KClass<*>,
    val qualifier: Qualifier? = null
) {

    companion object {
        fun of(type: KClass<*>, qualifier: Qualifier? = null): Key {
            return Key(type, qualifier)
        }
    }
}