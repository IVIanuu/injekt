package com.ivianuu.injekt

/**
 * Interface to distinct [Binding]s of the same type
 */
interface Qualifier

/**
 * A [Qualifier] which uses a [name]
 */
data class StringQualifier(val name: String) : Qualifier

/**
 * Returns a new [StringQualifier] for [name]
 */
fun named(name: String): StringQualifier = StringQualifier(name)