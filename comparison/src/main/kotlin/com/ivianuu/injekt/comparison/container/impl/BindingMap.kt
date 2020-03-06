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

package com.ivianuu.injekt.comparison.container.impl

import com.ivianuu.injekt.Key

class BindingMap(val entries: Map<Key, Binding<*>>) {

    fun <T> findBinding(key: Key): Binding<T>? {
        val binding = entries[key] as? Binding<T>
        if (binding != null) return binding

        if (key.type.isNullable) {
            val nullableKey = key.copy(type = key.type.copy(isNullable = true))
            return entries[nullableKey] as? Binding<T>
        }

        return null
    }

    fun <T> getBinding(key: Key): Binding<T> = findBinding(key)
        ?: error("Couldn't find binding for $key")

    companion object {
        operator fun invoke(
            vararg bindings: BindingMap
        ): BindingMap {
            return invoke(
                *bindings.map { it.entries }.toTypedArray()
            )
        }

        operator fun invoke(
            vararg bindings: Map<Key, Binding<*>>
        ): BindingMap {
            val finalBindings = mutableMapOf<Key, Binding<*>>()

            bindings.forEach { bindingMap ->
                bindingMap.forEach { (key, binding) ->
                    if (binding.overrideStrategy.check(
                            existsPredicate = { key in finalBindings },
                            errorMessage = { "Already declared key $key" }
                        )
                    ) {
                        finalBindings[key] = binding
                    }
                }
            }

            return BindingMap(
                finalBindings
            )
        }
    }
}
