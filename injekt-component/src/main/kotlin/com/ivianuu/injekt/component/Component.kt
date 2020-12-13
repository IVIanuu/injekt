@file:Suppress("UNCHECKED_CAST")

package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.givenOrElse
import kotlin.reflect.KClass

interface Component<N : Component.Name> {
    val storage: Storage<N>

    operator fun <K : Key<T>, T> get(element: K): T

    fun dispose()

    interface Name

    interface Key<T>

    interface Builder<N : Name> {
        operator fun <K : Key<T>, T> set(key: K, value: T): Builder<N>
        fun build(): Component<N>
    }
}

@Given fun <N : Component.Name> ComponentBuilder(
    elements: ComponentElements<N> = givenOrElse { emptyMap() },
): Component.Builder<N> = ComponentImpl.Builder(elements.toMap(mutableMapOf()))

typealias ComponentElements<@Suppress("unused") N> = Map<Component.Key<*>, Any?>

fun <N : Component.Name, K : Component.Key<T>, T> componentElementsOf(
    @Suppress("UNUSED_PARAMETER", "unused") name: KClass<N>,
    key: K,
    value: T,
): ComponentElements<N> = mapOf(key to value)

private class ComponentImpl<N : Component.Name>(
    private val elements: Map<Component.Key<*>, Any?>,
) : Component<N> {
    override val storage = Storage<N>()

    override fun <K : Component.Key<T>, T> get(element: K): T = elements[element] as T

    override fun dispose() {
        storage.dispose()
    }

    class Builder<N : Component.Name>(
        private val elements: MutableMap<Component.Key<*>, Any?>,
    ) : Component.Builder<N> {
        override fun <K : Component.Key<T>, T> set(key: K, value: T): Component.Builder<N> =
            apply { elements[key] = value }

        override fun build(): Component<N> = ComponentImpl(elements)
    }
}
