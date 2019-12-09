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
 * A [BindingSet] is the description of a "multi binding elements"
 *
 * A elements multi binding is a collection of instances of the same type
 * This allows to inject a elements of 'Set<E>'
 *
 * The contents of the elements can come from different modules
 *
 * The following is a typical usage of multi binding sets:
 *
 * ´´´
 * val fabricModule = Module {
 *     elements<AnalyticsEventHandler> {
 *         add<FabricAnalyticsEventHandler>()
 *     }
 * }
 *
 * val firebaseModule = Module {
 *     elements<AnalyticsEventHandler> {
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

class BindingSet<E> internal constructor(val elements: Set<Element<E>>) {
    data class Element<E>(
        val key: Key,
        val override: Boolean
    )
}

/**
 * Builder for a [BindingSet]
 *
 * @see ModuleBuilder.set
 */
class BindingSetBuilder<E> internal constructor(private val setKey: Key) {

    private val elements = mutableSetOf<BindingSet.Element<E>>()

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
     * Contributes a binding into this elements
     *
     * @param elementKey the key of the actual instance in the Component
     * @param override whether or not existing bindings can be overridden
     */
    fun add(elementKey: Key, override: Boolean = false) {
        add(BindingSet.Element(elementKey, override))
    }

    internal fun addAll(other: BindingSet<E>) {
        other.elements.forEach { add(it.key, it.override) }
    }

    private fun add(element: BindingSet.Element<E>) {
        check(element.override || elements.none { it.key == element.key }) {
            "Already declared ${element.key} in elements $setKey"
        }

        elements += element
    }

    internal fun build(): BindingSet<E> = BindingSet(elements)
}

internal class SetBindings(val sets: Map<Key, BindingSet<*>>)

internal class SetBindingsBuilder {

    private val setBuilders: MutableMap<Key, BindingSetBuilder<*>> = mutableMapOf()

    fun addAll(setBindings: SetBindings) {
        setBindings.sets.forEach { (setKey, set) ->
            val builder = getOrPut<Any?>(setKey)
            builder.addAll(set as BindingSet<Any?>)
        }
    }

    fun <E> getOrPut(setKey: Key): BindingSetBuilder<E> {
        return setBuilders.getOrPut(setKey) { BindingSetBuilder<Any?>(setKey) } as BindingSetBuilder<E>
    }

    fun build(): SetBindings {
        val sets = setBuilders
            .mapValues { it.value.build() }
        return SetBindings(sets)
    }

}