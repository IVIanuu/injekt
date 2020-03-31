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

/**
 * Provides instances of type [T]
 * For any type [T] that can be injected, you can also inject Provider<T>.
 * This enables providing multiple instances, lazy instances or optional retrieval of instances
 */
fun interface Provider<T> {
    /**
     * Provides an instance of type [T]
     */
    operator fun invoke(parameters: Parameters): T

    operator fun invoke(): T = invoke(emptyParameters())
}

@IntoComponent
private fun ComponentBuilder.providerJitFactory() {
    jitFactory { key, _ ->
        if (key.arguments.size != 1) return@jitFactory null
        if (key.classifier != Provider::class) return@jitFactory null
        val instanceKey = key.arguments.single()
            .copy(qualifier = key.qualifier)
        return@jitFactory Binding(key as Key<Provider<*>>) {
            KeyedProvider(this, instanceKey)
        }
    }
}

private class KeyedProvider<T>(
    private val component: Component,
    private val key: Key<T>
) : Provider<T> {
    override fun invoke(parameters: Parameters): T =
        component.get(key = key, parameters = parameters)
}
