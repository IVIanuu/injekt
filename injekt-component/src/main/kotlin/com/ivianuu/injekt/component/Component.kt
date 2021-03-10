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
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.common.ForTypeKey
import com.ivianuu.injekt.common.Scope
import com.ivianuu.injekt.common.TypeKey
import com.ivianuu.injekt.common.typeKeyOf

interface Component : Scope {
    val key: TypeKey<Component>

    fun <T> elementOrNull(key: TypeKey<T>): T?

    interface Builder<C : Component> {
        fun <T : Component> dependency(parent: T): Builder<C>
        fun <T> element(key: TypeKey<T>, factory: () -> T): Builder<C>
        fun initializer(initializer: ComponentInitializer<C>): Builder<C>
        fun build(): C
    }
}

fun <@ForTypeKey T> Component.element(): T =
    element(typeKeyOf())

fun <T> Component.element(key: TypeKey<T>): T {
    return elementOrNull(key)
        ?: error("No element for for $key in ${this.key}")
}

@Given
fun <@ForTypeKey C : Component> ComponentBuilder(
    @Given elementsFactory: (@Given C) -> Set<ComponentElement<C>>,
    @Given initializersFactory: (@Given C) -> Set<ComponentInitializer<C>>
): Component.Builder<C> = ComponentImpl.Builder(
    typeKeyOf<C>(),
    elementsFactory as (Component) -> Set<ComponentElement<Component>>,
    initializersFactory as (Component) -> Set<ComponentInitializer<Component>>,
)

fun <C : Component, @ForTypeKey T> Component.Builder<C>.element(factory: () -> T) =
    element(typeKeyOf(), factory)

typealias ComponentElement<@Suppress("unused") C> = Pair<TypeKey<*>, () -> Any?>

@Qualifier
annotation class ComponentElementBinding<C : Component>

@GivenSetElement
fun <@Given T : @ComponentElementBinding<C> S, @ForTypeKey S, @ForTypeKey C : Component>
        componentElementBindingImpl(@Given factory: () -> T): ComponentElement<C> =
    typeKeyOf<S>() to factory as () -> Any?

typealias ComponentInitializer<C> = (C) -> Unit

@Qualifier
annotation class ComponentInitializerBinding

@GivenSetElement
fun <@Given T : @ComponentInitializerBinding ComponentInitializer<C>, C : Component>
        componentInitializerBindingImpl(
    @Given initializer: T): ComponentInitializer<C> = initializer

@PublishedApi
internal class ComponentImpl(
    override val key: TypeKey<Component>,
    private val dependencies: List<Component>?,
    explicitElements: Map<TypeKey<*>, () -> Any?>?,
    injectedElements: (Component) -> Set<ComponentElement<*>>,
) : Component, Scope by Scope() {
    private val elements = (explicitElements ?: emptyMap()) + injectedElements(this)

    override fun <T> elementOrNull(key: TypeKey<T>): T? {
        if (key == this.key) return this as T
        elements[key]?.let { return it() as T }

        if (dependencies != null) {
            for (dependency in dependencies)
                dependency.elementOrNull(key)?.let { return it }
        }

        return null
    }

    class Builder<C : Component>(
        private val key: TypeKey<Component>,
        private val injectedElementsFactory: (Component) -> Set<ComponentElement<C>>,
        private val injectedInitializersFactory: (Component) -> Set<ComponentInitializer<C>>
    ) : Component.Builder<C> {
        private var dependencies: MutableList<Component>? = null
        private var elements: MutableMap<TypeKey<*>, () -> Any?>? = null
        private var initializers: MutableList<ComponentInitializer<C>>? = null

        override fun <T : Component> dependency(parent: T): Component.Builder<C> =
            apply {
                (dependencies ?: mutableListOf<Component>()
                    .also { dependencies = it }) += parent
            }

        override fun <T> element(key: TypeKey<T>, factory: () -> T): Component.Builder<C> =
            apply {
                (elements ?: mutableMapOf<TypeKey<*>, () -> Any?>()
                    .also { elements = it })[key] = factory
            }

        override fun initializer(initializer: ComponentInitializer<C>): Component.Builder<C> =
            apply {
                (initializers ?: mutableListOf<ComponentInitializer<C>>()
                    .also { initializers = it }) += initializer
            }

        override fun build(): C {
            val component = ComponentImpl(key, dependencies, elements, injectedElementsFactory) as C
            initializers?.forEach { it(component) }
            injectedInitializersFactory(component).forEach { it(component) }
            return component
        }
    }
}
