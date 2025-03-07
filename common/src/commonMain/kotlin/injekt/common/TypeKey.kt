package injekt.common

import kotlin.jvm.*

/**
 * A key for a Injekt type of [T] which can be used as a [Map] key or similar
 * For example it is used to store [Scoped] instances in a [Scope]
 */
@JvmInline value class TypeKey<out T>(val value: String)
