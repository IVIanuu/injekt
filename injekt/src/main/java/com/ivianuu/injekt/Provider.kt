/*
 * Copyright 2018 Manuel Wrage
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
 * Provides instances of [T]
 */
interface Provider<T> {
    operator fun invoke(parameters: ParametersDefinition? = null): T
}

// todo delete this class
internal class KeyedProvider<T>(
    private val component: Component,
    private val key: Key
) : Provider<T> {

    private var _binding: LinkedBinding<T>? = null

    override fun invoke(parameters: ParametersDefinition?): T {
        var binding = _binding
        if (binding == null) {
            binding = component.getBinding(key)
            _binding = binding
        }
        return binding(parameters)
    }
}