/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*

/**
 * A key for a Injekt type of [T] which can be used as a [Map] key or similar
 */
@JvmInline value class TypeKey<out T>(val value: String)

/**
 * Returns the [TypeKey] of [T]
 */
inline fun <T> typeKeyOf(@Inject x: TypeKey<T>): TypeKey<T> = x
