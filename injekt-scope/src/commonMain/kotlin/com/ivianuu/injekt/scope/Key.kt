package com.ivianuu.injekt.scope

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.common.SourceKey
import com.ivianuu.injekt.common.TypeKey

data class JoinedKey(val left: Any?, val right: Any?)

operator fun JoinedKey.plus(other: Any?): JoinedKey = JoinedKey(this, other)

inline fun joinedKeyOf(vararg keys: Any?): JoinedKey = keys.fold(JoinedKey(null, null)) { acc, next ->
  JoinedKey(acc, next)
}

@Tag annotation class DefaultSourceKey

@Provide inline fun defaultSourceKey(sourceKey: SourceKey): @DefaultSourceKey Any = sourceKey.value

@Tag annotation class DefaultTypeKey<T>

@Provide inline fun <T> defaultTypeKey(typeKey: TypeKey<T>): @DefaultTypeKey<T> Any = typeKey.value
