/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import kotlin.jvm.*
import kotlin.reflect.*

/**
 * A key for a type of [T] which can be used as a [Map] key or similar
 */
@JvmInline value class TypeKey<out T>(val type: KType)

/**
 * Returns the [TypeKey] of [T]
 */
@OptIn(ExperimentalStdlibApi::class)
@Provide inline fun <reified T> typeKeyOf(): TypeKey<T> = TypeKey(typeOf<T>())
