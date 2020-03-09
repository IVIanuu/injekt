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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.ComponentInitObserver
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.KeyWithOverrideInfo
import com.ivianuu.injekt.KeyedLazy
import com.ivianuu.injekt.KeyedProvider
import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.keyOf

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
class MultiBindingSetBuilder<E> internal constructor() {

    private val elements = mutableSetOf<KeyWithOverrideInfo>()

    inline fun <reified T : E> add(
        elementQualifiers: Qualifier = Qualifier.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ) {
        add(keyOf<T>(qualifier = elementQualifiers), duplicateStrategy)
    }

    fun add(elementKey: Key<*>, duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail) {
        add(
            KeyWithOverrideInfo(
                elementKey,
                duplicateStrategy
            )
        )
    }

    /**
     * Adds the [Binding] for [element] into this set
     */
    fun add(element: KeyWithOverrideInfo) {
        if (element.duplicateStrategy.check(
                existsPredicate = { elements.any { it.key == element.key } },
                errorMessage = { "Already declared element ${element.key}" }
            )
        ) {
            elements += element
        }
    }

    internal fun build(): MultiBindingSet<E> = elements
}

inline fun <reified E> ComponentBuilder.set(
    setQualifiers: Qualifier = Qualifier.None,
    noinline block: MultiBindingSetBuilder<E>.() -> Unit = {}
) {
    set(
        setKey = keyOf(
            classifier = Set::class,
            arguments = arrayOf(keyOf<E>()),
            qualifier = setQualifiers
        ),
        block = block
    )
}

/**
 * Runs the [block] in the scope of the [MultiBindingSetBuilder] for [setKey]
 *
 * @see MultiBindingSet
 */
fun <E> ComponentBuilder.set(
    setKey: Key<Set<E>>,
    block: MultiBindingSetBuilder<E>.() -> Unit = {}
) {
    var bindingProvider = bindings[setKey]?.provider as? SetBindingProvider<E>
    if (bindingProvider == null) {
        bindingProvider = SetBindingProvider(setKey)
        // bind the set
        bind(
            key = setKey,
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = bindingProvider
        )

        // provider set
        factory(
            key = keyOf<Set<Provider<E>>>(
                classifier = Set::class,
                arguments = arrayOf(
                    keyOf<Provider<E>>(
                        classifier = Provider::class,
                        arguments = arrayOf(setKey.arguments[0])
                    )
                )
            ),
            duplicateStrategy = DuplicateStrategy.Permit
        ) {
            bindingProvider.mergedSet!!
                .mapTo(mutableSetOf()) { element ->
                    KeyedProvider(
                        this,
                        element.key as Key<E>
                    )
                }
        }

        // lazy set
        factory(
            key = keyOf<Set<Lazy<E>>>(
                classifier = Set::class,
                arguments = arrayOf(
                    keyOf<Lazy<E>>(
                        classifier = Lazy::class,
                        arguments = arrayOf(setKey.arguments[0])
                    )
                )
            ),
            duplicateStrategy = DuplicateStrategy.Permit
        ) {
            bindingProvider.mergedSet!!
                .mapTo(mutableSetOf()) { element ->
                    KeyedLazy(
                        this,
                        element.key as Key<E>
                    )
                }
        }
    }

    bindingProvider.thisBuilder!!.block()
}

inline fun <reified T : E, reified E> BindingContext<T>.intoSet(
    setQualifier: Qualifier = Qualifier.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
): BindingContext<T> = intoSet(
    setKey = keyOf<Set<E>>(qualifier = setQualifier),
    duplicateStrategy = duplicateStrategy
)

/**
 * Adds the [BindingContext.binding] into to the set of [setKey]
 *
 * @see MultiBindingSet
 * @see MultiBindingSetBuilder
 */
fun <T, E> BindingContext<T>.intoSet(
    setKey: Key<Set<E>>,
    duplicateStrategy: DuplicateStrategy = binding.duplicateStrategy
): BindingContext<T> {
    componentBuilder.set(setKey = setKey) {
        add(
            elementKey = binding.key,
            duplicateStrategy = duplicateStrategy
        )
    }
    return this
}

private class SetBindingProvider<E>(
    private val setKey: Key<Set<E>>
) : (Component, Parameters) -> Set<E>,
    ComponentInitObserver {
    var thisBuilder: MultiBindingSetBuilder<E>? =
        MultiBindingSetBuilder()
    var thisSet: Set<KeyWithOverrideInfo>? = null
    var mergedSet: Set<KeyWithOverrideInfo>? = null

    override fun onInit(component: Component) {
        val mergedBuilder = MultiBindingSetBuilder<E>()

        component.getAllDependencies()
            .flatMap { dependency ->
                dependency.bindings[setKey]
                    ?.provider
                    ?.let { it as? SetBindingProvider<E> }
                    ?.thisSet ?: emptySet()
            }.forEach { element ->
                mergedBuilder.add(element)
            }

        thisSet = thisBuilder!!.build()
        thisBuilder = null

        thisSet!!.forEach { element ->
            mergedBuilder.add(element)
        }

        mergedSet = mergedBuilder.build()
    }

    override fun invoke(p1: Component, p2: Parameters): Set<E> =
        mergedSet!!.mapTo(mutableSetOf()) { p1.get(it.key as Key<E>) }
}
