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
 * val creditcardModule = Module {
 *     map<String, PaymentHandler> {
 *         put("creditcard", typeOf<CreditcardPaymentHandler>())
 *     }
 * }
 *
 * val paypalModule = Module {
 *     map<String, PaymentHandler> {
 *         put("paypal", typeOf<PaypalPaymentHandler>())
 *     }
 * }
 *
 * val component = Component {
 *     modules(creditcardModule, paypalModule)
 * }
 *
 * // will include both CreditcardPaymentHandler and PaypalPaymentHandler
 * val paymentHandlers = Component.get<Map<String, PaymentHandler>>()
 *
 * paymentHandlers.get(paymentMethod).processPayment(shoppingCart)
 * ´´´
 *
 * It's also possible to automatically retrieve a 'Map<K, Provider<V>>'
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
class MultiBindingMapBuilder<K, V> internal constructor(private val mapKey: Key) {
    private val entries = mutableMapOf<K, KeyWithOverrideInfo>()

    inline fun <reified T : V> put(
        entryKey: K,
        entryValueName: Any? = null,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail
    ) {
        put<T>(entryKey, typeOf(), entryValueName, overrideStrategy)
    }

    fun <T : V> put(
        entryKey: K,
        entryValueType: Type<T>,
        entryValueName: Any? = null,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail
    ) {
        val entryValueKey = keyOf(entryValueType, entryValueName)
        put(entryKey, entryValueKey, overrideStrategy)
    }

    fun put(
        entryKey: K,
        entryValueKey: Key,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail
    ) {
        put(entryKey, KeyWithOverrideInfo(entryValueKey, overrideStrategy))
    }

    /**
     * Contributes a binding into this map
     *
     * @param entryKey the key of the instance inside the map
     * @param entry the entry to add to this map
     */
    fun put(entryKey: K, entry: KeyWithOverrideInfo) {
        if (entry.overrideStrategy.check(
                existsPredicate = { entryKey in entries },
                errorMessage = { "Already declared $entryKey in map $mapKey" }
            )
        ) {
            entries[entryKey] = entry
        }
    }

    internal fun putAll(other: MultiBindingMap<K, V>) {
        other.entries.forEach { (key, entry) -> put(key, entry) }
    }

    internal fun build(): MultiBindingMap<K, KeyWithOverrideInfo> = entries
}

internal class MapOfProviderBinding<K, V>(
    private val entryKeys: Map<K, Key>
) : UnlinkedBinding<Map<K, Provider<V>>>() {
    override fun link(component: Component): LinkedBinding<Map<K, Provider<V>>> {
        return InstanceBinding(
            entryKeys.mapValues { component.getBinding(it.value) }
        )
    }
}

internal class MapOfValueBinding<K, V>(
    private val mapOfProviderKey: Key
) : UnlinkedBinding<Map<K, Lazy<V>>>() {
    override fun link(component: Component): LinkedBinding<Map<K, Lazy<V>>> =
        Linked(component.getBinding(mapOfProviderKey))

    private class Linked<K, V>(
        private val mapOfProviderBinding: Provider<Map<K, Provider<V>>>
    ) : LinkedBinding<Map<K, V>>() {
        override fun invoke(parameters: Parameters) = mapOfProviderBinding()
            .mapValues { it.value() }
    }
}

internal class MapOfLazyBinding<K, V>(
    private val mapOfProviderKey: Key
) : UnlinkedBinding<Map<K, Lazy<V>>>() {
    override fun link(component: Component): LinkedBinding<Map<K, Lazy<V>>> =
        Linked(component.getBinding(mapOfProviderKey))

    private class Linked<K, V>(
        private val mapOfProviderBinding: Provider<Map<K, Provider<V>>>
    ) : LinkedBinding<Map<K, Lazy<V>>>() {
        override fun invoke(parameters: Parameters) = mapOfProviderBinding()
            .mapValues { ProviderLazy(it.value) }
    }
}
