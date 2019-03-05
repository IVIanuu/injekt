package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * A key for a [Binding]
 */
sealed class Key {

    data class TypeKey(val type: KClass<*>) : Key() {
        override fun toString(): String {
            return "TypeKey(type=${type.java.name})"
        }
    }

    data class NameKey(val name: String) : Key()

    companion object {
        fun of(type: KClass<*>, name: String? = null): Key {
            return when {
                name != null -> of(name)
                else -> of(type)
            }
        }

        fun of(type: KClass<*>): Key = TypeKey(type)

        fun of(name: String): Key = NameKey(name)
    }
}