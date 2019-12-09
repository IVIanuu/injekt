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
 * A [BindingMap] is the description of a "multi binding map"
 *
 * A multi binding map is a keyed collection of instances of the same type
 * This allows to inject a map of 'Map<K, V>'
 *
 * The contents of the map can come from different modules
 *
 * The following is a typical usage of multi binding maps:
 *
 * ´´´
 * val creditcardModule = module {
 *     map<String, PaymentHandler> {
 *         put("creditcard", typeOf<CreditcardPaymentHandler>())
 *     }
 * }
 *
 * val paypalModule = module {
 *     map<String, PaymentHandler> {
 *         put("paypal", typeOf<PaypalPaymentHandler>())
 *     }
 * }
 *
 * val component = component {
 *     modules(creditcardModule, paypalModule)
 * }
 *
 * // will include both CreditcardPaymentHandler and PaypalPaymentHandler
 * val paymentHandlers = component.get<Map<String, PaymentHandler>>()
 *
 * paymentHandlers.get(paymentMethod).processPayment(shoppingCart)
 * ´´´
 *
 * It's also possible to automatically retrieve a 'Map<K, Provider<V>>'
 * or a 'Map<K, Lazy<V>>'
 *
 *
 * @see Module.map
 */
class BindingMap<K, V> internal constructor(private val mapKey: Key) {
    private val entries = mutableMapOf<K, Entry>()

    inline fun <reified T : V> put(
        entryKey: K,
        entryValueName: Any? = null,
        override: Boolean = false
    ) {
        put<T>(entryKey, typeOf(), entryValueName, override)
    }

    fun <T : V> put(
        entryKey: K,
        entryValueType: Type<T>,
        entryValueName: Any? = null,
        override: Boolean = false
    ) {
        val entryValueKey = keyOf(entryValueType, entryValueName)
        put(entryKey, entryValueKey, override)
    }

    /**
     * Contributes a binding into this map
     *
     * @param entryKey the key of the instance inside the map
     * @param entryValueKey the key of the actual instance in the component
     * @param override whether or not existing bindings can be overridden
     */
    fun put(
        entryKey: K,
        entryValueKey: Key,
        override: Boolean = false
    ) {
        put(entryKey, Entry(entryValueKey, override))
    }

    internal fun getBindingMap(): Map<K, Key> = entries.mapValues { it.value.entryValueKey }

    internal fun putAll(other: BindingMap<K, V>) {
        other.entries.forEach { (key, entry) -> put(key, entry) }
    }

    private fun put(entryKey: K, entry: Entry) {
        check(entry.override || entryKey !in entries) {
            "Already declared $entryKey in map $mapKey"
        }
        entries[entryKey] = entry
    }

    private class Entry(
        val entryValueKey: Key,
        val override: Boolean
    )
}

internal class MapBindings {

    private val maps: MutableMap<Key, BindingMap<*, *>> = mutableMapOf()

    fun putAll(mapBindings: MapBindings) {
        mapBindings.maps.forEach { (mapKey, map) ->
            val thisMap = get<Any?, Any?>(mapKey)
            thisMap.putAll(map as BindingMap<Any?, Any?>)
        }
    }

    fun <K, V> get(mapKey: Key): BindingMap<K, V> =
        maps.getOrPut(mapKey) { BindingMap<K, V>(mapKey) } as BindingMap<K, V>

    fun getAll(): Map<Key, BindingMap<*, *>> = maps
}
