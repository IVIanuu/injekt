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
 * via a [ComponentBuilder] or annotating classes with [Factory] or [Single]
 *
 * @see Factory
 * @see Single
 */
class Binding<T>(
    /**
     * The which is used to identify this binding
     */
    val key: Key<T>,
    /**
     * How overrides should be handled
     */
    val overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    /**
     * Provides instances of [T]
     */
    val provider: BindingProvider<T>
) {

    fun copy(
        key: Key<T> = this.key,
        overrideStrategy: OverrideStrategy = this.overrideStrategy,
        provider: BindingProvider<T> = this.provider
    ) = Binding(key, overrideStrategy, provider)

}

typealias BindingProvider<T> = Component.(Parameters) -> T

interface BindingFactory<T> {
    fun create(): Binding<T>
}
