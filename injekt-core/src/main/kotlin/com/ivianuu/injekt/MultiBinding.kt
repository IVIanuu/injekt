package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

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
 * @ModuleMarker
 * val creditCardModule = Module {
 *     map<String, PaymentHandler> {
 *         put("creditCard", keyOf<CreditCardPaymentHandler>())
 *     }
 * }
 *
 * @ModuleMarker
 * val paypalModule = Module {
 *     map<String, PaymentHandler> {
 *         put("paypal", keyOf<PaypalPaymentHandler>())
 *     }
 * }
 *
 * val component = Component()
 *
 * // will include both CreditCardPaymentHandler and PaypalPaymentHandler
 * val paymentHandlers = Component.get<Map<String, PaymentHandler>>()
 *
 * paymentHandlers.get(paymentMethod).processPayment(shoppingCart)
 * ´´´
 *
 * It's also possible retrieve a 'Map<K, Provider<V>>'
 * or a 'Map<K, Lazy<V>>'
 *
 *
 * @see ModuleDsl.map
 * @see MapDsl
 */
class MapDsl<K, V> internal constructor() {
    private val entries = mutableMapOf<K, Binding<out V>>()

    fun put(entryKey: K, entryValueKey: Key<out V>) {
        put(entryKey) { get(entryValueKey) }
    }

    inline fun put(entryKey: K, entryBindingDefinition: BindingDefinition<out V>): Unit =
        injektIntrinsic()

    fun put(entryKey: K, entryBinding: Binding<out V>) {
        check(entryKey !in entries) {
            "Already declared $entryKey"
        }
        entries[entryKey] = entryBinding
    }

    internal fun build(): Map<K, Binding<V>> = entries as Map<K, Binding<V>>
}

inline fun <reified K, reified V> ModuleDsl.map(
    mapQualifier: KClass<*>? = null,
    block: MapDsl<K, V>.() -> Unit = {}
): Unit = injektIntrinsic()

/**
 * A multi binding set is a set of bindings
 * This allows to inject 'Set<E>'
 *
 * The contents of the set can come from different modules
 *
 * The following is a typical usage of multi binding sets:
 *
 * ´´´
 * @ModuleMarker
 * val fabricModule = Module {
 *     set<AnalyticsEventHandler> {
 *         add<FabricAnalyticsEventHandler>()
 *     }
 * }
 *
 * @ModuleMarker
 * val firebaseModule = Module {
 *     set<AnalyticsEventHandler> {
 *         add<FirebaseAnalyticsEventHandler>()
 *     }
 * }
 *
 * val component = Component()
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
class SetDsl<E> internal constructor() {

    private val elements = mutableMapOf<Key<out E>, Binding<out E>>()

    inline fun <reified T : E> add(elementQualifier: KClass<*>? = null): Unit = injektIntrinsic()

    inline fun <reified T : E> add(elementKey: Key<T>) {
        add(elementKey) { get(elementKey) }
    }

    inline fun <reified T : E> add(
        elementQualifier: KClass<*>? = null,
        elementBindingDefinition: BindingDefinition<T>
    ): Unit = injektIntrinsic()

    inline fun <reified T : E> add(
        elementKey: Key<T>,
        elementBindingDefinition: BindingDefinition<T>
    ): Unit = injektIntrinsic()

    inline fun <reified T : E> add(
        elementQualifier: KClass<*>? = null,
        elementBinding: Binding<T>
    ): Unit = injektIntrinsic()

    fun <T : E> add(
        elementKey: Key<T>,
        elementBinding: Binding<T>
    ) {
        check(elementBinding !in elements) {
            "Already declared element $elementBinding"
        }
        elements[elementKey] = elementBinding
    }

    internal fun build(): Map<Key<out E>, Binding<out E>> = elements

}

inline fun <reified E> ModuleDsl.set(
    setQualifier: KClass<*>? = null,
    noinline block: SetDsl<E>.() -> Unit = {}
): Unit = injektIntrinsic()
