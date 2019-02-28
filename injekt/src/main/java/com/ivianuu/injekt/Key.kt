package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * A key for a [Binding]
 */
data class Key(
    val type: KClass<*>,
    val name: String? = null
) {
    override fun toString(): String {
        return if (name == null) {
            "type=${type.java.name}"
        } else {
            "name=$name"
        }
    }
}