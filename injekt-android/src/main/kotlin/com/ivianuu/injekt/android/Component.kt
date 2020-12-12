@file:Suppress("UNCHECKED_CAST")

package com.ivianuu.injekt.android

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given

interface Component<K : Component.Key<*>> {
    val storage: Storage
    operator fun <TK : Key<T>, T> get(element: TK): T
    interface Key<T>
    interface Builder<K : Key<*>> {
        operator fun <TK : Key<T>, T> set(key: TK, value: T): Builder<K>
        fun build(): Component<K>
    }
}

@Given fun <K : Component.Key<*>> ComponentBuilder(
    elements: ComponentElements<K> = given,
): Component.Builder<K> = ComponentImpl.Builder(elements.toMap(mutableMapOf()))

typealias ComponentElements<K> = Map<Any?, Any?>

fun <K : Component.Key<T>, T> componentElementsOf(
    key: K,
    value: T,
): ComponentElements<K> = mapOf(key to value)

private class ComponentImpl<K : Component.Key<*>>(
    private val elements: Map<Any?, Any?>,
) : Component<K> {
    override val storage = Storage()

    override fun <TK : Component.Key<T>, T> get(element: TK): T = elements[element] as T

    class Builder<K : Component.Key<*>>(
        private val elements: MutableMap<Any?, Any?>,
    ) : Component.Builder<K> {
        override fun <TK : Component.Key<T>, T> set(key: TK, value: T): Component.Builder<K> =
            apply { elements[key] = value }

        override fun build(): Component<K> = ComponentImpl(elements)
    }
}
