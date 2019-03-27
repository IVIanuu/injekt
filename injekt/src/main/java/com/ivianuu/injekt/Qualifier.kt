package com.ivianuu.injekt

/**
 * Interface to distinct [Binding]s of the same type
 */
interface Qualifier

/**
 * A [Qualifier] which uses a [name]
 */
open class StringQualifier(val name: String) : Qualifier {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StringQualifier) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String = name

}

/**
 * Returns a new [StringQualifier] for [name]
 */
fun named(name: String): StringQualifier = StringQualifier(name)