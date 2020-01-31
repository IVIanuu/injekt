/*
 * Copyright 2019 Manuel Wrage
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
 * Used to retrieve all dependencies in a [UnlinkedBinding.link] call
 */
inline class Linker(internal val component: Component) {

    inline fun <reified T> get(name: Any? = null): LinkedBinding<T> =
        get(type = typeOf(), name = name)

    fun <T> get(type: Type<T>, name: Any? = null): LinkedBinding<T> =
        get(key = keyOf(type, name))

    /**
     * Obtain a linked binding
     *
     * @param key the for the binding
     * @return the linked binding
     */
    fun <T> get(key: Key): LinkedBinding<T> = component.getBinding(key)

    /**
     * Obtain a linked binding or null
     *
     * @param key the for the binding
     * @return the linked binding
     */
    fun <T> getOrNull(key: Key): LinkedBinding<T>? = component.getBindingOrNull(key)
}
