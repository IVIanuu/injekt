package injekt.common

import injekt.inject
import kotlin.jvm.*

/**
 * A key for a Injekt type of [T] which can be used as a [Map] key or similar
 */
@JvmInline value class TypeKey<out T>(val value: String)

/**
 * Returns the [TypeKey] of [T]
 */
inline fun <T> typeKeyOf(x: TypeKey<T> = inject): TypeKey<T> = x
