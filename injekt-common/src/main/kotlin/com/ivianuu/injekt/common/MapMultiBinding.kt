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

import com.ivianuu.injekt.AbstractBindingProvider
import com.ivianuu.injekt.Binding
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
import com.jakewharton.confundus.unsafeCast

/**
 * A builder for a "multi binding map"
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

    fun <T : V> put(
        entryKey: K,
        entryValueKey: Key<T>,
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

    internal fun put(entryKey: K, entry: KeyWithOverrideInfo) {
        if (entry.duplicateStrategy.check(
                existsPredicate = { entryKey in entries },
                errorMessage = { "Already declared $entryKey" }
            )
        ) {
            entries[entryKey] = entry
        }
    }

    internal fun build(): Map<K, KeyWithOverrideInfo> = entries
}

inline fun <reified K, reified V> ComponentBuilder.map(
    mapQualifier: Qualifier = Qualifier.None,
    block: MultiBindingMapBuilder<K, V>.() -> Unit = {}
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
 */
inline fun <K, V> ComponentBuilder.map(
    mapKey: Key<Map<K, V>>,
    block: MultiBindingMapBuilder<K, V>.() -> Unit = {}
) {
    getMapBuilder(mapKey).block()
}

@PublishedApi
internal fun <K, V> ComponentBuilder.getMapBuilder(mapKey: Key<Map<K, V>>): MultiBindingMapBuilder<K, V> {
    val mapOfKeyWithOverrideInfo = keyOf<Map<K, KeyWithOverrideInfo>>(
        classifier = Map::class,
        arguments = arrayOf(
            mapKey.arguments.first(),
            keyOf<KeyWithOverrideInfo>(qualifier = Qualifier(mapKey))
        ),
        qualifier = mapKey.qualifier
    )

    var bindingProvider = bindings[mapOfKeyWithOverrideInfo]?.provider as? MapBindingProvider<K, V>
    if (bindingProvider == null) {
        bindingProvider = MapBindingProvider(mapOfKeyWithOverrideInfo)

        // bind the map with keys
        bind(
            Binding(
                key = mapOfKeyWithOverrideInfo,
                duplicateStrategy = DuplicateStrategy.Override,
                provider = bindingProvider
            )
        )

        // value map
        factory(
            key = mapKey,
            duplicateStrategy = DuplicateStrategy.Override
        ) {
            get(key = mapOfKeyWithOverrideInfo)
                .mapValues { (_, value) ->
                    get(value.key).unsafeCast()
                }
        }

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
                ),
                qualifier = mapKey.qualifier
            ),
            duplicateStrategy = DuplicateStrategy.Override
        ) {
            get(key = mapOfKeyWithOverrideInfo)
                .mapValues { (_, value) ->
                    get(
                        key = keyOf(
                            classifier = Provider::class,
                            arguments = arrayOf(value.key),
                            qualifier = value.key.qualifier
                        )
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
                ),
                qualifier = mapKey.qualifier
            ),
            duplicateStrategy = DuplicateStrategy.Override
        ) {
            get(key = mapOfKeyWithOverrideInfo)
                .mapValues { (_, value) ->
                    get(
                        key = keyOf(
                            classifier = Lazy::class,
                            arguments = arrayOf(value.key),
                            qualifier = value.key.qualifier
                        )
                    )
                }
        }
    }

    return bindingProvider.thisBuilder!!
}

private class MapBindingProvider<K, V>(
    private val mapOfKeyWithOverrideInfo: Key<Map<K, KeyWithOverrideInfo>>
) : AbstractBindingProvider<Map<K, KeyWithOverrideInfo>>() {
    var thisBuilder: MultiBindingMapBuilder<K, V>? =
        MultiBindingMapBuilder()
    var thisMap: Map<K, KeyWithOverrideInfo>? = null
    var mergedMap: Map<K, KeyWithOverrideInfo>? = null

    override fun onAttach(component: Component) {
        checkNotNull(thisBuilder)
        val mergedBuilder = MultiBindingMapBuilder<K, V>()

        component.getAllParents()
            .flatMap { parent ->
                (parent.bindings[mapOfKeyWithOverrideInfo]
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

    override fun invoke(component: Component, parameters: Parameters): Map<K, KeyWithOverrideInfo> {
        return mergedMap!!
    }
}
