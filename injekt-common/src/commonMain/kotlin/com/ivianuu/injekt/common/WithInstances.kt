/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*

/**
 * Calls the specified function [block] with [a] as injectable and returns it's result
 */
inline fun <A, R> withInstances(a: A, block: InstanceProvider1<A>.() -> R): R =
  block(InstanceProvider1(a))

class InstanceProvider1<A>(@Provide val a: A)

/**
 * Calls the specified function [block] with [a] and [b] as injectable and returns it's result
 */
inline fun <A, B, R> withInstances(a: A, b: B, block: InstanceProvider2<A, B>.() -> R) =
  block(InstanceProvider2(a, b))

class InstanceProvider2<A, B>(@Provide val a: A, @Provide val b: B)

/**
 * Calls the specified function [block] with [a], [b] and [c] as injectable and returns it's result
 */
inline fun <A, B, C, R> withInstances(
  a: A,
  b: B,
  c: C,
  block: InstanceProvider3<A, B, C>.() -> R
) = block(InstanceProvider3(a, b, c))

class InstanceProvider3<A, B, C>(@Provide val a: A, @Provide val b: B, @Provide val c: C)

/**
 * Calls the specified function [block] with [a], [b], [c] and [d] as injectable and returns it's result
 */
inline fun <A, B, C, D, R> withInstances(
  a: A,
  b: B,
  c: C,
  d: D,
  block: InstanceProvider4<A, B, C, D>.() -> R,
) = block(InstanceProvider4(a, b, c, d))

class InstanceProvider4<A, B, C, D>(
  @Provide val a: A,
  @Provide val b: B,
  @Provide val c: C,
  @Provide val d: D
)

/**
 * Calls the specified function [block] with [a], [b], [c], [d] and [e] as injectable and returns it's result
 */
inline fun <A, B, C, D, E, R> withInstances(
  a: A,
  b: B,
  c: C,
  d: D,
  e: E,
  block: InstanceProvider5<A, B, C, D, E>.() -> R,
) = block(InstanceProvider5(a, b, c, d, e))

class InstanceProvider5<A, B, C, D, E>(
  @Provide val a: A,
  @Provide val b: B,
  @Provide val c: C,
  @Provide val d: D,
  @Provide val e: E,
)
