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
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.keyOf

/**
 * A multi binding set is a set of bindings
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
class MultiBindingSetBuilder<E> internal constructor() {

    private val elements = mutableSetOf<KeyWithOverrideInfo>()

    inline fun <reified T : E> add(
        elementQualifier: Qualifier = Qualifier.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ) {
        add(keyOf<T>(qualifier = elementQualifier), duplicateStrategy)
    }

    fun add(elementKey: Key<*>, duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail) {
        add(
            KeyWithOverrideInfo(
                elementKey,
                duplicateStrategy
            )
        )
    }

    internal fun add(element: KeyWithOverrideInfo) {
        if (element.duplicateStrategy.check(
                existsPredicate = { elements.any { it.key == element.key } },
                errorMessage = { "Already declared element ${element.key}" }
            )
        ) {
            elements += element
        }
    }

    internal fun build(): Set<KeyWithOverrideInfo> = elements
}

inline fun <reified E> ComponentBuilder.set(
    setQualifier: Qualifier = Qualifier.None,
    block: MultiBindingSetBuilder<E>.() -> Unit = {}
) {
    set(
        setKey = keyOf(
            classifier = Set::class,
            arguments = arrayOf(keyOf<E>()),
            qualifier = setQualifier
        ),
        block = block
    )
}

/**
 * Runs the [block] in the scope of the [MultiBindingSetBuilder] for [setKey]
 */
inline fun <E> ComponentBuilder.set(
    setKey: Key<Set<E>>,
    block: MultiBindingSetBuilder<E>.() -> Unit = {}
) {
    getSetBuilder(setKey).block()
}

@PublishedApi
internal fun <E> ComponentBuilder.getSetBuilder(
    setKey: Key<Set<E>>
): MultiBindingSetBuilder<E> {
    val setOfKeyWithOverrideInfoKey = keyOf<Set<KeyWithOverrideInfo>>(
        classifier = Set::class,
        arguments = arrayOf(
            keyOf<KeyWithOverrideInfo>(qualifier = Qualifier(setKey))
        ),
        qualifier = setKey.qualifier
    )

    var bindingProvider = bindings[setOfKeyWithOverrideInfoKey]?.provider as? SetBindingProvider<E>
    if (bindingProvider == null) {
        bindingProvider = SetBindingProvider(setOfKeyWithOverrideInfoKey)

        // bind the set
        bind(
            Binding(
                key = setOfKeyWithOverrideInfoKey,
                duplicateStrategy = DuplicateStrategy.Override,
                provider = bindingProvider
            )
        )

        // value set
        factory(
            key = setKey,
            duplicateStrategy = DuplicateStrategy.Override
        ) {
            get(setOfKeyWithOverrideInfoKey)
                .mapTo(mutableSetOf()) { element ->
                    get(element.key) as E
                }
        }

        // provider set
        factory(
            key = keyOf<Set<Provider<E>>>(
                classifier = Set::class,
                arguments = arrayOf(
                    keyOf<Provider<E>>(
                        classifier = Provider::class,
                        arguments = arrayOf(setKey.arguments[0])
                    )
                ),
                qualifier = setKey.qualifier
            ),
            duplicateStrategy = DuplicateStrategy.Override
        ) {
            get(setOfKeyWithOverrideInfoKey)
                .mapTo(mutableSetOf()) { element ->
                    get(
                        key = keyOf(
                            classifier = Provider::class,
                            arguments = arrayOf(element.key),
                            qualifier = element.key.qualifier
                        )
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
                ),
                qualifier = setKey.qualifier
            ),
            duplicateStrategy = DuplicateStrategy.Override
        ) {
            get(setOfKeyWithOverrideInfoKey)
                .mapTo(mutableSetOf()) { element ->
                    get(
                        key = keyOf(
                            classifier = Lazy::class,
                            arguments = arrayOf(element.key),
                            qualifier = element.key.qualifier
                        )
                    )
                }
        }
    }

    return bindingProvider.thisBuilder!!
}

private class SetBindingProvider<E>(
    private val setOfKeyWithOverrideInfoKey: Key<Set<KeyWithOverrideInfo>>
) : BindingProvider<Set<KeyWithOverrideInfo>> {
    var thisBuilder: MultiBindingSetBuilder<E>? =
        MultiBindingSetBuilder()
    var thisSet: Set<KeyWithOverrideInfo>? = null
    private var mergedSet: Set<KeyWithOverrideInfo>? = null

    override fun onAttach(component: Component) {
        checkNotNull(thisBuilder)

        val mergedBuilder = MultiBindingSetBuilder<E>()

        component.getAllParents()
            .flatMap { parent ->
                parent.bindings[setOfKeyWithOverrideInfoKey]
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

    override fun invoke(component: Component, parameters: Parameters): Set<KeyWithOverrideInfo> =
        mergedSet!!
}
