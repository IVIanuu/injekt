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
 * @author Manuel Wrage (IVIanuu)
 */
object GlobalPool {

    private val bindingsByScope = mutableMapOf<Any, MutableSet<Binding<*>>>()
    private val unscopedBindings = mutableSetOf<Binding<*>>()

    init {
        loadGeneratedBindings() // todo make this optional
    }

    fun loadGeneratedBindings() {
        FastServiceLoader.load(MultiCreator::class.java, javaClass.classLoader)
            .flatMap { it.create() }
            .forEach { binding ->
                if (binding.scope != null) {
                    bindingsByScope.getOrPut(binding.scope) { mutableSetOf() }.add(binding)
                } else {
                    unscopedBindings.add(binding)
                }
            }
    }

    fun getBindingsForScope(scope: Any?): Set<Binding<*>> = bindingsByScope[scope] ?: emptySet()

    fun getUnscopedBindings(): Set<Binding<*>> = unscopedBindings
}