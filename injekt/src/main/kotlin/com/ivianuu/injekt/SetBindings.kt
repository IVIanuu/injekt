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
 * A [BindingSet] is the description of a "multi binding set"
 *
 * A set multi binding is a collection of instances of the same type
 * This allows to inject a set of 'Set<E>'
 *
 * The contents of the set can come from different modules
 *
 * The following is a typical usage of multi binding sets:
 *
 * ´´´
 * val fabricModule = module {
 *     set<AnalyticsEventHandler> {
 *         add<FabricAnalyticsEventHandler>()
 *     }
 * }
 *
 * val firebaseModule = module {
 *     set<AnalyticsEventHandler> {
 *         add<FirebaseAnalyticsEventHandler>()
 *     }
 * }
 *
 * val component = component {
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
 * @see Module.set
 */
class BindingSet<E> internal constructor(private val setKey: Key) {

    private val entries = mutableMapOf<Key, Entry>()

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

    /**
     * Contributes a binding into this set
     *
     * @param elementKey the key of the actual instance in the component
     * @param override whether or not existing bindings can be overridden
     */
    fun add(elementKey: Key, override: Boolean = false) {
        check(override || elementKey !in entries) {
            "Already declared $elementKey in set $setKey"
        }

        entries[elementKey] = Entry(override)
    }

    internal fun addAll(other: BindingSet<E>) {
        other.entries.forEach { (key, entry) -> add(key, entry.override) }
    }

    internal fun getBindingSet(): Set<Key> = entries.keys

    private class Entry(val override: Boolean)
}

internal class SetBindings {

    private val sets: MutableMap<Key, BindingSet<*>> = mutableMapOf()

    fun addAll(setBindings: SetBindings) {
        setBindings.sets.forEach { (setKey, set) ->
            val thisSet = get<Any?>(setKey)
            thisSet.addAll(set as BindingSet<Any?>)
        }
    }

    fun <E> get(setKey: Key): BindingSet<E> {
        return sets.getOrPut(setKey) { BindingSet<Any?>(setKey) } as BindingSet<E>
    }

    fun getAll(): Map<Key, BindingSet<*>> = sets
}
