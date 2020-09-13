/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt

import kotlin.reflect.KClass

annotation class Given(val scope: KClass<out ContextName> = AnyContext::class)

fun <@ForKey T> Context.givenOrNull(key: Key<T> = keyOf()): T? =
    givenProviderOrNull(key)?.let { runReader { it() } }

fun <@ForKey T> Context.given(key: Key<T> = keyOf()): T =
    givenProviderOrNull(key)?.let { runReader { it() } }
        ?: error("No given found for '$key'")

@Reader
fun <@ForKey T> givenOrNull(key: Key<T> = keyOf()): T? =
    currentContext.givenOrNull(key)

@Reader
fun <@ForKey T> given(key: Key<T> = keyOf()): T = currentContext.given(key)

@Reader
fun <@ForKey P1, @ForKey R> given(
    p1: P1
): R = currentContext.given(keyOf<(P1) -> R>())(p1)

@Reader
fun <@ForKey P1, @ForKey P2, @ForKey R> given(
    p1: P1,
    p2: P2
): R = currentContext.given(keyOf<(P1, P2) -> R>())(p1, p2)

@Reader
fun <@ForKey P1, @ForKey P2, @ForKey P3, @ForKey R> given(
    p1: P1,
    p2: P2,
    p3: P3
): R = currentContext.given(keyOf<(P1, P2, P3) -> R>())(p1, p2, p3)

@Reader
fun <@ForKey P1, @ForKey P2, @ForKey P3, @ForKey P4, @ForKey R> given(
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4
): R = currentContext.given(keyOf<(P1, P2, P3, P4) -> R>())(p1, p2, p3, p4)

@Reader
fun <@ForKey P1, @ForKey P2, @ForKey P3, @ForKey P4, @ForKey P5, @ForKey R> given(
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5
): R = currentContext.given(keyOf<(P1, P2, P3, P4, P5) -> R>())(p1, p2, p3, p4, p5)

@Reader
fun <@ForKey P1, @ForKey P2, @ForKey P3, @ForKey P4, @ForKey P5, @ForKey P6, @ForKey R> given(
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6
): R = given(keyOf<(P1, P2, P3, P4, P5, P6) -> R>())(p1, p2, p3, p4, p5, p6)

@Reader
fun <@ForKey P1, @ForKey P2, @ForKey P3, @ForKey P4, @ForKey P5, @ForKey P6, @ForKey P7, @ForKey R> given(
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7
): R = given(keyOf<(P1, P2, P3, P4, P5, P6, P7) -> R>())(p1, p2, p3, p4, p5, p6, p7)

@Reader
fun <@ForKey P1, @ForKey P2, @ForKey P3, @ForKey P4, @ForKey P5, @ForKey P6, @ForKey P7, @ForKey P8, @ForKey R> given(
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8
): R = given(keyOf<(P1, P2, P3, P4, P5, P6, P7, P8) -> R>())(p1, p2, p3, p4, p5, p6, p7, p8)

@Reader
fun <@ForKey P1, @ForKey P2, @ForKey P3, @ForKey P4, @ForKey P5, @ForKey P6, @ForKey P7, @ForKey P8, @ForKey P9, @ForKey R> given(
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8,
    p9: P9
): R = given(keyOf<(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R>())(p1, p2, p3, p4, p5, p6, p7, p8, p9)
