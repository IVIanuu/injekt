package com.ivianuu.injekt

inline class Key<T>(private val value: Int)

inline fun <reified T> keyOf(qualifier: Qualifier? = null): Key<T> =
    keyOf(typeOf(), qualifier)

inline fun <T> keyOf(
    type: Type<T>,
    qualifier: Qualifier? = null
): Key<T> {
    var result = type.hashCode()
    result = 31 * result + qualifier.hashCode()
    return Key(result)
}
