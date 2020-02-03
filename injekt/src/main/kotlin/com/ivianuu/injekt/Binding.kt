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
sealed class Binding<T> {

    /**
     * Overrides existing bindings with the same key
     */
    var overrideStrategy = OverrideStrategy.Fail
        internal set

    /**
     * Creates the instance in the moment the component get's created
     */
    var eager = false
        internal set

    /**
     * Creates instances in the bound scope
     */
    var scoped = false
        internal set

    /**
     * Returns a [LinkedBinding] and get's all required dependencies from the [component]
     *
     * @param component the linker where to get required bindings from
     */
    protected abstract fun link(component: Component): LinkedBinding<T>

    internal open fun performLink(component: Component): LinkedBinding<T> {
        val linked = link(component)
        if (this !is ProxyBinding) {
            linked.overrideStrategy = overrideStrategy
            linked.eager = eager
            linked.scoped = scoped
        }

        return linked
    }
}

abstract class UnlinkedBinding<T> : Binding<T>()

abstract class LinkedBinding<T> : Binding<T>(), Provider<T> {
    final override fun link(component: Component): LinkedBinding<T> = this
    final override fun performLink(component: Component): LinkedBinding<T> = this
}
