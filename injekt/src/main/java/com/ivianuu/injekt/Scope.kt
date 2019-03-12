package com.ivianuu.injekt

/**
 * @author Manuel Wrage (IVIanuu)
 */
interface Scope

open class NamedScope(val name: String) : Scope {
    override fun toString(): String = name
}