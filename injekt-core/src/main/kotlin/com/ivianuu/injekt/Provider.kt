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

/**
 * Provides instances of type [T]
 * For any type [T] that can be injected, you can also inject Provider<T>.
 * This enables providing multiple instances, lazy instances or optional retrieval of instances
 */
fun interface Provider<T> {
    /**
     * Returns an instance of type [T]
     */
    operator fun invoke(parameters: Parameters): T

    operator fun invoke(): T = invoke(emptyParameters())
}

class ProviderDsl {
    fun <T> get(key: Key<T>): T = error("Implemented as an intrinsic")
    inline fun <reified T> get(qualifier: KClass<*>? = null): T = get(keyOf(qualifier))
}

typealias ProviderDefinition<T> = ProviderDsl.(Parameters) -> T

internal class KeyedProvider<T>(
    private val linker: Linker,
    private val key: Key<T>
) : Provider<T> {
    private var provider: Provider<T>? = null

    override fun invoke(parameters: Parameters): T {
        if (provider == null) {
            provider = linker.get(key)
        }
        return provider!!(parameters)
    }
}
