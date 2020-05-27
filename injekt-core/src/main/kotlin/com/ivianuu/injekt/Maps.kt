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

import com.ivianuu.injekt.internal.injektIntrinsic

interface MapDsl<M : Map<K, V>, K, V> {
    fun <T : V> put(entryKey: K)
    // todo fun <T : V> put(entryKey: K, definition: ProviderDefinition<T>)
}

fun <K, V> map(block: MapDsl<Map<K, V>, K, V>.() -> Unit = {}): Unit = injektIntrinsic()

@JvmName("qualifiedMap")
fun <M : Map<K, V>, K, V> map(block: MapDsl<M, K, V>.() -> Unit = {}): Unit = injektIntrinsic()
