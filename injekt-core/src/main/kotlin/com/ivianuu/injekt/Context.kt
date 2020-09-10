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

interface Context {
    fun <T> givenProviderOrNull(key: Key<T>): @Reader (() -> T)?
}

internal class ContextImpl(
    private val parent: Context?,
    private val providers: Map<Key<*>, @Reader () -> Any?>
) : Context {
    @Suppress("UNCHECKED_CAST")
    override fun <T> givenProviderOrNull(key: Key<T>): @Reader (() -> T)? =
        providers[key] as? @Reader (() -> T)? ?: parent?.givenProviderOrNull(key)
}

@Reader
val currentContext: Context
    get() = error("Intrinsic")
