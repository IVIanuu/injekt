package com.ivianuu.injekt

/**
 * @author Manuel Wrage (IVIanuu)
 */
interface Qualifier

data class StringQualifier(val name: String) : Qualifier

fun named(name: String): StringQualifier = StringQualifier(name)