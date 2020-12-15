@file:Suppress("UNCHECKED_CAST")

package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given

interface Component<N : Component.Name> {
    val name: N

    val storage: Storage<N>

    fun <T : Any> getOrNull(key: Key<T>): T?

    fun <N : Name> getDependencyOrNull(name: N): Component<N>?

    interface Name

    interface Key<T : Any>

    interface Builder<N : Name> {
        fun dependency(parent: Component<*>): Builder<N>
        fun <T : Any> element(key: Key<T>, value: T): Builder<N>
        fun build(): Component<N>
    }
}

fun <T : Any> ComponentKey(): Component.Key<T> = DefaultKey()
private class DefaultKey<T : Any> : Component.Key<T>

operator fun <T : Any> Component<*>.get(key: Component.Key<T>): T = getOrNull(key)
    ?: error("No value for for $key in ${this.name}")


fun <N : Component.Name> Component<*>.getDependency(name: N): Component<N> =
    getDependencyOrNull(name)
        ?: error("No value for for $name in ${this.name}")

fun Component<*>.dispose() {
    storage.dispose()
}

@Given fun <N : Component.Name> ComponentBuilder(
    name: N = given,
    injectedElements: (Component<N>) -> Set<ComponentElement<N>> = given,
): Component.Builder<N> = ComponentImpl.Builder(name, injectedElements)

inline fun <N : Component.Name> Component(
    name: N = given,
    noinline injectedElements: (Component<N>) -> Set<ComponentElement<N>> = given,
    block: Component.Builder<N>.() -> Unit = {},
): Component<N> = ComponentBuilder(name, injectedElements).apply(block).build()

typealias ComponentElement<@Suppress("unused") N> = Pair<Component.Key<*>, Any>

fun <N : Component.Name, T : Any> componentElement(
    @Suppress("UNUSED_PARAMETER", "unused") name: N,
    key: Component.Key<T>,
    value: T,
): ComponentElement<N> = key to value

@PublishedApi internal class ComponentImpl<N : Component.Name>(
    override val name: N,
    private val dependencies: List<Component<*>>,
    private val explicitElements: Map<Component.Key<*>, Any?>,
    private val injectedElements: (Component<N>) -> Set<ComponentElement<N>>,
) : Component<N> {
    private val elements = explicitElements + injectedElements(this)

    override val storage = Storage<N>()

    override fun <T : Any> getOrNull(key: Component.Key<T>): T? {
        elements[key]?.let { return it as T }

        for (dependency in dependencies)
            dependency.getOrNull(key)?.let { return it }

        return null
    }

    override fun <N : Component.Name> getDependencyOrNull(name: N): Component<N>? {
        for (dependency in dependencies)
            if (dependency.name == name) return dependency as Component<N>

        for (dependency in dependencies)
            dependency.getDependencyOrNull(name)?.let { return it }

        return null
    }

    class Builder<N : Component.Name>(
        private val name: N,
        private val injectedElements: (Component<N>) -> Set<ComponentElement<N>>,
    ) : Component.Builder<N> {
        private val dependencies = mutableListOf<Component<*>>()
        private val elements = mutableMapOf<Component.Key<*>, Any?>()

        override fun dependency(parent: Component<*>): Component.Builder<N> = apply {
            dependencies += parent
        }

        override fun <T : Any> element(key: Component.Key<T>, value: T): Component.Builder<N> =
            apply {
                elements[key] = value
            }

        override fun build(): Component<N> =
            ComponentImpl(name, dependencies, elements, injectedElements)
    }
}
