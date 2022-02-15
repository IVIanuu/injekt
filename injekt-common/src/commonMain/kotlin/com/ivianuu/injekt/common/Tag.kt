/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import kotlin.jvm.*

interface Tag<T> {
  val _value: Any?
}

@JvmName("unwrap0")
inline operator fun <A : Tag<T>, T> A.invoke(): T = _value as T

@JvmName("unwrap1")
inline operator fun <A : Tag<B>, B : Tag<T>, T> A.invoke(): T =
  invoke<A, B>()._value as T

@JvmName("unwrap2")
inline operator fun <A : Tag<B>, B : Tag<C>, C : Tag<T>, T> A.invoke(): T =
  invoke<A, B>().invoke<B, C>()._value as T

@JvmName("unwrap3")
inline operator fun <A : Tag<B>, B : Tag<C>, C : Tag<D>, D : Tag<T>, T> A.invoke(): T =
  invoke<A, B>().invoke<B, C>().invoke<C, D>()._value as T

@JvmName("unwrap4")
inline operator fun <A : Tag<B>, B : Tag<C>, C : Tag<D>, D : Tag<E>, E : Tag<T>, T> A.invoke(): T =
  invoke<A, B>().invoke<B, C>().invoke<C, D>().invoke<D, E>()._value as T
