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
 * val component = Component {
 *     modules(fabricModule, firebaseModule)
 * }
 *
 * // will include both FabricAnalyticsEventHandler and FirebaseAnalyticsEventHandler
 * val analyticsEventHandlers = component.get<Set<AnalyticsEventHandler>>()
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
 * @see ComponentBuilder.set
 */
// todo ir use * instead of Any?
typealias MultiBindingSet<E> = Set<KeyWithOverrideInfo>

/**
 * Builder for a [MultiBindingSet]
 *
 * @see ComponentBuilder.set
 */
class MultiBindingSetBuilder<E> internal constructor(private val setKey: Key<Set<E>>) {

    private val elements = mutableSetOf<KeyWithOverrideInfo>()

    inline fun <reified T : E> add(
        elementName: Any? = null,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ) {
        add(keyOf<T>(name = elementName), duplicateStrategy)
    }

    fun add(elementKey: Key<*>, duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail) {
        add(KeyWithOverrideInfo(elementKey, duplicateStrategy))
    }

    /**
     * Contributes a binding into this set
     *
     * @param element the element to add to this set
     */
    fun add(element: KeyWithOverrideInfo) {
        if (element.duplicateStrategy.check(
                existsPredicate = { elements.any { it.key == element.key } },
                errorMessage = { "Already declared ${element.key} in elements $setKey" }
            )
        ) {
            elements += element
        }
    }

    internal fun addAll(other: MultiBindingSet<E>) {
        other.forEach { add(it) }
    }

    internal fun build(): MultiBindingSet<E> = elements
}
