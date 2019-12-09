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
 * A [MultiBindingSet] is a set of bindings
 * This allows to inject 'Set<E>'
 *
 * The contents of the set can come from different modules
 *
 * The following is a typical usage of multi binding sets:
 *
 * ´´´
 * val fabricModule = Module {
 *     set<AnalyticsEventHandler> {
 *         add<FabricAnalyticsEventHandler>()
 *     }
 * }
 *
 * val firebaseModule = Module {
 *     set<AnalyticsEventHandler> {
 *         add<FirebaseAnalyticsEventHandler>()
 *     }
 * }
 *
 * val Component = Component {
 *     modules(fabricModule, firebaseModule)
 * }
 *
 * // will include both FabricAnalyticsEventHandler and FirebaseAnalyticsEventHandler
 * val analyticsEventHandlers = Component.get<Set<AnalyticsEventHandler>>()
 *
 * analyticsEventHandlers.forEach { handler ->
 *     handler.handleEvent(MyEvent())
 * }
 * ´´´
 *
 * It's also possible to automatically retrieve a 'Set<Provider<E>>'
 * or a 'Set<Lazy<E>>'
 *
 *
 * @see ModuleBuilder.set
 */
typealias MultiBindingSet<E> = Set<KeyWithOverrideInfo>

/**
 * Builder for a [MultiBindingSet]
 *
 * @see ModuleBuilder.set
 */
class MultiBindingSetBuilder<E> internal constructor(private val setKey: Key) {

    private val elements = mutableSetOf<KeyWithOverrideInfo>()

    inline fun <reified T : E> add(
        elementName: Any? = null,
        override: Boolean = false
    ) {
        add<T>(typeOf(), elementName, override)
    }

    fun <T : E> add(
        elementType: Type<T>,
        elementName: Any? = null,
        override: Boolean = false
    ) {
        add(keyOf(elementType, elementName), override)
    }

    fun add(elementKey: Key, override: Boolean = false) {
        add(KeyWithOverrideInfo(elementKey, override))
    }

    /**
     * Contributes a binding into this set
     *
     * @param element the element to add to this set
     */
    fun add(element: KeyWithOverrideInfo) {
        check(element.override || elements.none { it.key == element.key }) {
            "Already declared ${element.key} in elements $setKey"
        }

        elements += element
    }

    internal fun addAll(other: MultiBindingSet<E>) {
        other.forEach { add(it) }
    }

    internal fun build(): MultiBindingSet<E> = elements
}
