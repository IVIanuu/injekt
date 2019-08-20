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

sealed class Binding<T> {

    var override = false
        internal set

    internal var unscoped = false
    internal var linkPerformed = false

    abstract fun link(linker: Linker): LinkedBinding<T>

    internal open fun performLink(linker: Linker): LinkedBinding<T> {
        val linked = link(linker)
        // some bindings such as the proxy binding are unlinked bindings only
        // so they return already linked bindings
        // we only copy the state if it's the first link
        if (!linked.linkPerformed) {
            linked.linkPerformed = true
            linked.override = override
            linked.unscoped = unscoped
        }
        return linked
    }

}

abstract class UnlinkedBinding<T> : Binding<T>()

abstract class LinkedBinding<T> : Binding<T>(), Provider<T> {
    final override fun link(linker: Linker): LinkedBinding<T> = this
    override fun performLink(linker: Linker): LinkedBinding<T> = this
}