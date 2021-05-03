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

class GivenTuple1<A>(@Given val a: A)

/**
 * Calls the specified function [block] with [a] as given and returns it's result
 */
inline fun <A, R> withGivens(a: A, block: GivenTuple1<A>.() -> R): R = block(GivenTuple1(a))

class GivenTuple2<A, B>(@Given val a: A, @Given val b: B)

/**
 * Calls the specified function [block] with [a] and [b] as given and returns it's result
 */
inline fun <A, B, R> withGivens(a: A, b: B, block: GivenTuple2<A, B>.() -> R) =
    block(GivenTuple2(a, b))

class GivenTuple3<A, B, C>(@Given val a: A, @Given val b: B, @Given val c: C)

/**
 * Calls the specified function [block] with [a], [b] and [c] as given and returns it's result
 */
inline fun <A, B, C, R> withGivens(a: A, b: B, c: C, block: GivenTuple3<A, B, C>.() -> R) =
    block(GivenTuple3(a, b, c))

class GivenTuple4<A, B, C, D>(@Given val a: A, @Given val b: B, @Given val c: C, @Given val d: D)

/**
 * Calls the specified function [block] with [a], [b], [c] and [d] as given and returns it's result
 */
inline fun <A, B, C, D, R> withGivens(
    a: A,
    b: B,
    c: C,
    d: D,
    block: GivenTuple4<A, B, C, D>.() -> R,
) = block(GivenTuple4(a, b, c, d))

class GivenTuple5<A, B, C, D, E>(
    @Given val a: A,
    @Given val b: B,
    @Given val c: C,
    @Given val d: D,
    @Given val e: E,
)

/**
 * Calls the specified function [block] with [a], [b], [c], [d] and [e] as given and returns it's result
 */
inline fun <A, B, C, D, E, R> withGivens(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    block: GivenTuple5<A, B, C, D, E>.() -> R,
) = block(GivenTuple5(a, b, c, d, e))
