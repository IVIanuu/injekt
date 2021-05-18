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

// todo make tuples value classes

class ProviderTuple1<A>(@Provide val a: A)

/**
 * Calls the specified function [block] with [a] as given and returns it's result
 */
inline fun <A, R> using(a: A, block: ProviderTuple1<A>.() -> R): R = block(ProviderTuple1(a))

class ProviderTuple2<A, B>(@Provide val a: A, @Provide val b: B)

/**
 * Calls the specified function [block] with [a] and [b] as given and returns it's result
 */
inline fun <A, B, R> using(a: A, b: B, block: ProviderTuple2<A, B>.() -> R) =
  block(ProviderTuple2(a, b))

class ProviderTuple3<A, B, C>(@Provide val a: A, @Provide val b: B, @Provide val c: C)

/**
 * Calls the specified function [block] with [a], [b] and [c] as given and returns it's result
 */
inline fun <A, B, C, R> using(a: A, b: B, c: C, block: ProviderTuple3<A, B, C>.() -> R) =
  block(ProviderTuple3(a, b, c))

class ProviderTuple4<A, B, C, D>(@Provide val a: A, @Provide val b: B, @Provide val c: C, @Provide val d: D)

/**
 * Calls the specified function [block] with [a], [b], [c] and [d] as given and returns it's result
 */
inline fun <A, B, C, D, R> using(
  a: A,
  b: B,
  c: C,
  d: D,
  block: ProviderTuple4<A, B, C, D>.() -> R,
) = block(ProviderTuple4(a, b, c, d))

class ProviderTuple5<A, B, C, D, E>(
  @Provide val a: A,
  @Provide val b: B,
  @Provide val c: C,
  @Provide val d: D,
  @Provide val e: E,
)

/**
 * Calls the specified function [block] with [a], [b], [c], [d] and [e] as given and returns it's result
 */
inline fun <A, B, C, D, E, R> using(
  a: A,
  b: B,
  c: C,
  d: D,
  e: E,
  block: ProviderTuple5<A, B, C, D, E>.() -> R,
) = block(ProviderTuple5(a, b, c, d, e))
