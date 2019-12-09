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
 * A binding knows how to create a concrete instance of a type
 * It also holds additional information about the declaration
 * You typically don't access them directly but instead declare dependencies
 * via the [Module]s or annotating classes [Factory] [Single]
 *
 * @see Module
 * @see Factory
 * @see Single
 */
sealed class Binding<T> {

    /**
     * Overrides existing bindings with the same key
     */
    var override = false
        internal set

    /**
     * Creates the instance in the moment the Component get's created
     */
    var eager = false
        internal set

    /**
     * Creates instances in the bound scope
     */
    var scoped = false
        internal set

    internal var linkPerformed = false

    /**
     * Returns a [LinkedBinding] and get's all required dependencies from the [linker]
     *
     * @param linker the linker where to get required bindings from
     *
     * @see UnlinkedMapOfLazyBinding
     */
    protected abstract fun link(linker: Linker): LinkedBinding<T>

    internal open fun performLink(linker: Linker): LinkedBinding<T> {
        val linked = link(linker)
        // some bindings such as the proxy binding are unlinked bindings only
        // they return existing linked bindings
        // we only copy the state if it's the first link
        if (!linked.linkPerformed) {
            linked.linkPerformed = true
            linked.override = override
            linked.eager = eager
            linked.scoped = scoped
        }
        return linked
    }
}

abstract class UnlinkedBinding<T> : Binding<T>()

abstract class LinkedBinding<T> : Binding<T>(), Provider<T> {
    final override fun link(linker: Linker): LinkedBinding<T> = this
    final override fun performLink(linker: Linker): LinkedBinding<T> {
        linkPerformed = true
        return this
    }
}
