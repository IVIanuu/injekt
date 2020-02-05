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
 * A binding knows how to create a concrete instance of a type
 * it also holds additional information about the declaration
 * you typically don't access this class directly but instead declare dependencies
 * via a [ModuleBuilder] or annotating classes with [Factory] or [Single]
 *
 * @see Module
 * @see Factory
 * @see Single
 */
class Binding<T>(
    /**
     * The key used to store and retrieve this binding
     */
    val key: Key,
    /**
     * The kind of this binding
     */
    val kind: Kind,
    /**
     * The scoping for this binding
     */
    val scoping: Scoping = Scoping.Unscoped,
    /**
     * Overrides existing bindings with the same key
     */
    val overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,

    /**
     * The factory which creates instances for this binding
     */
    val instanceFactory: InstanceFactory<T>
) {
    fun createInstance(component: Component) = kind.wrap(
        binding = this, instance = instanceFactory.create(), component = component
    )
}

interface BindingFactory<T> {
    fun create(): Binding<T>
}
