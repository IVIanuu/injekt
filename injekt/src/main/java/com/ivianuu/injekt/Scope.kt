package com.ivianuu.injekt

/**
 * Scope marker
 */
interface Scope

/**
 * A [Scope] which uses the [name] in [toString]
 */
open class StringScope(val name: String) : Scope {
    override fun toString(): String = name
}