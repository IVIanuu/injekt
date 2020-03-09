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
 * A [MultiBindingMap] is the description of a "multi binding map"
 *
 * A multi binding map is a keyed collection of instances of the same type
 * This allows to inject a map of 'Map<K, V>'
 *
 * The contents of the map can come from different modules
 *
 * The following is a typical usage of multi binding maps:
 *
 * ´´´
 * val creditCardModule = Module {
 *     map<String, PaymentHandler> {
 *         put("creditCard", keyOf<CreditcardPaymentHandler>())
 *     }
 * }
 *
 * val paypalModule = Module {
 *     map<String, PaymentHandler> {
 *         put("paypal", keyOf<PaypalPaymentHandler>())
 *     }
 * }
 *
 * val component = Component {
 *     modules(creditCardModule, paypalModule)
 * }
 *
 * // will include both CreditcardPaymentHandler and PaypalPaymentHandler
 * val paymentHandlers = Component.get<Map<String, PaymentHandler>>()
 *
 * paymentHandlers.get(paymentMethod).processPayment(shoppingCart)
 * ´´´
 *
 * It's also possible retrieve a 'Map<K, Provider<V>>'
 * or a 'Map<K, Lazy<V>>'
 *
 *
 * @see ComponentBuilder.map
 * @see MultiBindingMapBuilder
 */
// todo ir use * instead of Any?
typealias MultiBindingMap<K, V> = Map<K, KeyWithOverrideInfo>

/**
 * Builder for a [MultiBindingSet]
 *
 * @see ComponentBuilder.map
 */
class MultiBindingMapBuilder<K, V> internal constructor() {
    private val entries = mutableMapOf<K, KeyWithOverrideInfo>()

    inline fun <reified T : V> put(
        entryKey: K,
        entryValueQualifier: Qualifier = Qualifier.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ) {
        put(
            entryKey,
            keyOf<T>(qualifier = entryValueQualifier), duplicateStrategy
        )
    }

    fun put(
        entryKey: K,
        entryValueKey: Key<*>,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ) {
        put(
            entryKey,
            KeyWithOverrideInfo(
                entryValueKey,
                duplicateStrategy
            )
        )
    }

    /**
     * Adds the [Binding] for [entry] into this map
     */
    fun put(entryKey: K, entry: KeyWithOverrideInfo) {
        if (entry.duplicateStrategy.check(
                existsPredicate = { entryKey in entries },
                errorMessage = { "Already declared $entryKey" }
            )
        ) {
            entries[entryKey] = entry
        }
    }

    internal fun build(): MultiBindingMap<K, KeyWithOverrideInfo> = entries
}

inline fun <reified K, reified V> ComponentBuilder.map(
    mapQualifier: Qualifier = Qualifier.None,
    noinline block: MultiBindingMapBuilder<K, V>.() -> Unit = {}
) {
    map(
        mapKey = keyOf(
            classifier = Map::class,
            arguments = arrayOf(
                keyOf<K>(),
                keyOf<V>()
            ),
            qualifier = mapQualifier
        ), block = block
    )
}

/**
 * Runs the [block] in the scope of the [MultiBindingMapBuilder] for [mapKey]
 *
 * @see MultiBindingMap
 */
fun <K, V> ComponentBuilder.map(
    mapKey: Key<Map<K, V>>,
    block: MultiBindingMapBuilder<K, V>.() -> Unit = {}
) {
    var bindingProvider = bindings[mapKey]?.provider as? MapBindingProvider<K, V>
    if (bindingProvider == null) {
        bindingProvider = MapBindingProvider(mapKey)
        // bind the map
        bind(
            key = mapKey,
            duplicateStrategy = DuplicateStrategy.Override,
            provider = bindingProvider
        )

        // provider map
        factory(
            key = keyOf<Map<K, Provider<V>>>(
                classifier = Map::class,
                arguments = arrayOf(
                    mapKey.arguments[0],
                    keyOf<Provider<V>>(
                        classifier = Provider::class,
                        arguments = arrayOf(mapKey.arguments[1])
                    )
                )
            ),
            duplicateStrategy = DuplicateStrategy.Override
        ) {
            bindingProvider.mergedMap!!
                .mapValues { (_, value) ->
                    KeyedProvider(
                        this,
                        value.key as Key<V>
                    )
                }
        }

        // lazy map
        factory(
            key = keyOf<Map<K, Lazy<V>>>(
                classifier = Map::class,
                arguments = arrayOf(
                    mapKey.arguments[0],
                    keyOf<Lazy<V>>(
                        classifier = Lazy::class,
                        arguments = arrayOf(mapKey.arguments[1])
                    )
                )
            ),
            duplicateStrategy = DuplicateStrategy.Override
        ) {
            bindingProvider.mergedMap!!
                .mapValues { (_, value) ->
                    KeyedLazy(
                        this,
                        value.key as Key<V>
                    )
                }
        }
    }

    bindingProvider.thisBuilder!!.block()
}

private class MapBindingProvider<K, V>(
    private val mapKey: Key<Map<K, V>>
) : (Component, Parameters) -> Map<K, V>,
    ComponentInitObserver {
    var thisBuilder: MultiBindingMapBuilder<K, V>? =
        MultiBindingMapBuilder()
    var thisMap: Map<K, KeyWithOverrideInfo>? = null
    var mergedMap: Map<K, KeyWithOverrideInfo>? = null

    override fun onInit(component: Component) {
        val mergedBuilder = MultiBindingMapBuilder<K, V>()

        component.getAllDependencies()
            .flatMap { dependency ->
                (dependency.bindings[mapKey]
                    ?.provider
                    ?.let { it as? MapBindingProvider<K, V> }
                    ?.thisMap ?: emptyMap()).entries
            }.forEach { (key, value) ->
                mergedBuilder.put(key, value)
            }

        thisMap = thisBuilder!!.build()
        thisBuilder = null

        thisMap!!.forEach { (key, value) ->
            mergedBuilder.put(key, value)
        }

        mergedMap = mergedBuilder.build()
    }

    override fun invoke(p1: Component, p2: Parameters): Map<K, V> {
        return mergedMap!!
            .mapValues { p1.get(it.value.key as Key<V>) }
    }
}
