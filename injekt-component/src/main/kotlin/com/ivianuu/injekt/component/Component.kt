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

@file:Suppress("UNCHECKED_CAST")

package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.Macro
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.common.ForKey
import com.ivianuu.injekt.common.Key
import com.ivianuu.injekt.common.Scope
import com.ivianuu.injekt.common.keyOf

interface Component : Scope {
    val key: Key<Component>

    fun <T> getOrNull(key: Key<T>): T?

    interface Builder<C : Component> {
        fun <T : Component> dependency(parent: T): Builder<C>
        fun <T> element(key: Key<T>, factory: () -> T): Builder<C>
        fun build(): C
    }
}

fun <@ForKey T> Component.get(): T {
    val key = keyOf<T>()
    return getOrNull(key)
        ?: error("No value for for $key in ${this.key}")
}

@Given fun <@ForKey C : Component> ComponentBuilder(
    @Given injectedElements: (@Given C) -> Set<ComponentElement<C>>,
): Component.Builder<C> = ComponentImpl.Builder(
    keyOf<C>(),
    injectedElements as (Component) -> Set<ComponentElement<*>>
)

fun <C : Component, @ForKey T> Component.Builder<C>.element(factory: () -> T) =
    element(keyOf(), factory)

typealias ComponentElement<@Suppress("unused") C> = Pair<Key<*>, () -> Any?>

@Qualifier annotation class ComponentElementBinding<C : Component>

@Macro
@GivenSetElement
fun <T : @ComponentElementBinding<C> S, @ForKey S, @ForKey C : Component>
        componentElementBindingImpl(@Given factory: () -> T): ComponentElement<C> =
    keyOf<S>() to factory as () -> Any?

@PublishedApi internal class ComponentImpl(
    override val key: Key<Component>,
    private val dependencies: List<Component>,
    explicitElements: Map<Key<*>, () -> Any?>,
    injectedElements: (@Given Component) -> Set<ComponentElement<*>>,
) : Component, Scope by Scope() {
    private val elements = explicitElements + injectedElements(this)

    override fun <T> getOrNull(key: Key<T>): T? {
        if (key == this.key) return this as T
        elements[key]?.let { return it() as T }

        for (dependency in dependencies)
            dependency.getOrNull(key)?.let { return it }

        return null
    }

    class Builder<C : Component>(
        private val key: Key<Component>,
        private val injectedElements: (Component) -> Set<ComponentElement<*>>,
    ) : Component.Builder<C> {
        private val dependencies = mutableListOf<Component>()
        private val elements = mutableMapOf<Key<*>, () -> Any?>()

        override fun <T : Component> dependency(parent: T): Component.Builder<C> =
            apply { dependencies += parent }

        override fun <T> element(key: Key<T>, factory: () -> T): Component.Builder<C> =
            apply { elements[key] = factory }

        override fun build(): C =
            ComponentImpl(key, dependencies, elements, injectedElements) as C
    }
}
