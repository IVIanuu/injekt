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

class LazyBinding<T>(private val key: Key) : Binding<Lazy<T>> {

    private lateinit var binding: Binding<T>

    override fun link(linker: Linker) {
        // todo pass key directly
        binding = linker.get(key.type as Type<T>, key.name)
    }

    override fun get(parameters: ParametersDefinition?): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
        binding.get()
    }

}

class ProviderBinding<T>(private val key: Key) : Binding<Provider<T>> {

    private lateinit var binding: Binding<T>

    override fun link(linker: Linker) {
        // todo pass key directly
        binding = linker.get(key.type as Type<T>, key.name)
    }

    override fun get(parameters: ParametersDefinition?): Provider<T> = provider {
        binding.get(it)
    }

}