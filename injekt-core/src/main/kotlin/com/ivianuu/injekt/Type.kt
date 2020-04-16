package com.ivianuu.injekt

import kotlin.reflect.KClass
import kotlin.reflect.KType

inline class Type<T>(private val value: Int)

inline fun <reified T> typeOf(): Type<T> = kotlin.reflect.typeOf<T>().asType<T>()

@PublishedApi
internal fun <T> KType.asType(): Type<T> {
    return if (arguments.isNotEmpty()) {
        val args = arrayOfNulls<Type<Any?>>(arguments.size)
        for (index in arguments.indices) {
            args[index] = arguments[index].type?.asType() ?: typeOf(Any::class, isNullable = true)
        }
        typeOf((classifier as? KClass<*> ?: Any::class), isMarkedNullable)
    } else {
        typeOf((classifier as? KClass<*> ?: Any::class), isMarkedNullable)
    }
}

fun <T> typeOf(
    classifier: KClass<*>,
    isNullable: Boolean = false
): Type<T> {
    var result = classifier.java.name.hashCode()
    result = 31 * result + isNullable.hashCode()
    return Type(result)
}

fun <T> typeOf(
    classifier: KClass<*>,
    isNullable: Boolean = false,
    arguments: Array<Type<*>>
): Type<T> {
    var result = classifier.java.name.hashCode()
    result = 31 * result + isNullable.hashCode()
    result = 31 * result + arguments.contentHashCode()
    return Type(result)
}
