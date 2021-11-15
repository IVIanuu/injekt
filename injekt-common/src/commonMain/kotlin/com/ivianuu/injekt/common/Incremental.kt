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

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag

@Tag annotation class Incremental {
  fun interface Factory<T, E> {
    fun create(
      combined: Array<T>?,
      elements: Array<E>?,
      collectionElements: Array<Collection<E>>?
    ): T

    companion object {
      @Provide fun <E> list() = Factory<List<E>, E> { _, elements, collectionElements ->
        mutableListOf<E>().apply {
          elements?.forEach { add(it) }
          collectionElements?.forEach { addAll(it) }
        }
      }

      @Provide fun <K, V> map() = Factory<Map<K, V>, Pair<K, V>> { _, entries, collectionEntries ->
        mutableMapOf<K, V>().apply {
          entries?.forEach { put(it.first, it.second) }
          collectionEntries?.forEach { putAll(it) }
        }
      }
    }
  }
}
