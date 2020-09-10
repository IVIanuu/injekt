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

inline fun <reified T> Context.givenOrNull(): T? =
    givenOrNull(keyOf())

fun <T> Context.givenOrNull(key: Key<T>): T? =
    givenProviderOrNull(key)?.let { runReader { it() } }

inline fun <reified T> Context.given(): T = given(keyOf())

fun <T> Context.given(key: Key<T>): T = givenProviderOrNull(key)?.let { runReader { it() } }
    ?: error("No given found for '$key'")

@JvmName("readerGiven")
@Reader
inline fun <reified T> given(): T = currentContext.given()

@JvmName("readerGiven")
@Reader
fun <T> given(key: Key<T>): T = currentContext.given(key)
