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
 *
 * You typically don't access this class directly but instead declare dependencies
 * via a [ComponentBuilder] or annotating classes with [Factory] or [Single]
 *
 * @see Factory
 * @see Single
 */
data class Binding<T> private constructor(
    /**
     * The key which is used to identify this binding
     */
    val key: Key<T>,
    /**
     * Behavior applied to the [provider]
     */
    val behavior: Behavior = Behavior.None,
    /**
     * How overrides should be handled
     */
    val duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    /**
     * Creates instances for this binding
     */
    val provider: BindingProvider<T>
) {
    companion object {
        /**
         * Returns a new [Binding] instance
         */
        operator fun <T> invoke(
            key: Key<T>,
            behavior: Behavior = Behavior.None,
            duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
            provider: BindingProvider<T>
        ): Binding<T> = Binding(
            key = key,
            behavior = behavior,
            duplicateStrategy = duplicateStrategy,
            provider = behavior.foldIn(provider) { currentProvider, currentBehavior ->
                currentBehavior.apply(currentProvider)
            }
        )
    }
}

/**
 * Provides instances of [T]
 */
typealias BindingProvider<T> = Component.(Parameters) -> T

/**
 * Base class for special [BindingProvider]s
 */
abstract class DelegatingBindingProvider<T>(
    val delegate: BindingProvider<T>
) : (Component, Parameters) -> T, ComponentInitObserver {

    private var initialized = false

    override fun onInit(component: Component) {
        check(!initialized) { "Binding providers should not be reused" }
        initialized = true
        (delegate as? ComponentInitObserver)?.onInit(component)
    }

    override fun invoke(p1: Component, p2: Parameters): T = delegate(p1, p2)
}

/**
 * Used by the codegen
 */
interface BindingFactory<T> {
    fun create(): Binding<T>
}
