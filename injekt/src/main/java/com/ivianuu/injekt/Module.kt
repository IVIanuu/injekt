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
 * A module is a collection of [Binding]s to drive [Component]s
 */
interface Module {
    val bindings: Map<Key, Binding<*>> get() = emptyMap()
    val mapBindings: Map<Key, Map<Any?, Binding<*>>> get() = emptyMap()
    val setBindings: Map<Key, Set<Binding<*>>> get() = emptyMap()
}

internal class DefaultModule(
    override val bindings: Map<Key, Binding<*>> = emptyMap(),
    override val mapBindings: Map<Key, Map<Any?, Binding<*>>> = emptyMap(),
    override val setBindings: Map<Key, Set<Binding<*>>> = emptyMap()
) : Module

