package com.ivianuu.injekt

/**
 * Scope marker
 */
interface Scope

/**
 * A [Scope] which uses the [name] in [toString]
 */
open class StringScope(val name: String) : Scope {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StringScope) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String = name

}