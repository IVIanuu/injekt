package com.ivianuu.injekt

import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface Key<T>

inline fun <reified T> keyOf(): Key<T> = KTypeKey(typeOf<T>())

data class KTypeKey<T>(val kType: KType) : Key<T>
