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
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.common.ForTypeKey
import com.ivianuu.injekt.common.Scope
import com.ivianuu.injekt.common.TypeKey
import com.ivianuu.injekt.common.typeKeyOf

/**
 * A hierarchical construct with a lifecycle which hosts a map of keys to elements
 * which can be retrieved via [Component.elementOrNull] or [Component.element]
 */
interface Component : Scope {
    /**
     * The exact type key of this component
     */
    val key: TypeKey<Component>

    /**
     * Returns the element [T] for [key] or null
     */
    fun <T> elementOrNull(key: TypeKey<T>): T?

    /**
     * Construct a [Component] instance
     */
    interface Builder<C : Component> {
        /**
         * Adds [parent] as a dependency
         */
        fun <T : Component> dependency(parent: T): Builder<C>
        /**
         * Registers a element for [key] which will be provided by [factory]
         */
        fun <T> element(key: TypeKey<T>, factory: () -> T): Builder<C>
        /**
         * Registers the [initializer]
         */
        fun initializer(initializer: ComponentInitializer<C>): Builder<C>
        /**
         * Returns the configured [Component] instance
         */
        fun build(): C
    }
}

fun <@ForTypeKey T> Component.element(): T =
    element(typeKeyOf())

/**
 * Returns the element [T] for [key] or throws
 */
fun <T> Component.element(key: TypeKey<T>): T = elementOrNull(key)
    ?: error("No element for for $key in ${this.key}")

/**
 * Returns a new [Component.Builder] instance
 */
fun <@ForTypeKey C : Component> ComponentBuilder(
    @Given elements: (@Given C) -> Set<ComponentElement<C>>,
    @Given initializers: (@Given C) -> Set<ComponentInitializer<C>>
): Component.Builder<C> = ComponentImpl.Builder(
    typeKeyOf<C>(),
    elements as (Component) -> Set<ComponentElement<Component>>,
    initializers as (Component) -> Set<ComponentInitializer<Component>>,
)

@Given
fun <@ForTypeKey C : Component> componentBuilder(
    @Given elements: (@Given C) -> Set<ComponentElement<C>> = { emptySet() },
    @Given initializers: (@Given C) -> Set<ComponentInitializer<C>> = { emptySet() }
) = ComponentBuilder(elements, initializers)

fun <C : Component, @ForTypeKey T> Component.Builder<C>.element(factory: () -> T) =
    element(typeKeyOf(), factory)

typealias ComponentElement<@Suppress("unused") C> = Pair<TypeKey<*>, () -> Any?>

/**
 * Registers the declaration a element in the [Component] [C]
 *
 * Example:
 * ```
 * @ComponentElementBinding<AppComponent>
 * @Given
 * class MyAppDeps(@Given api: Api, @Given database: Database)
 *
 * fun runApp(@Given appComponent: AppComponent) {
 *    val deps = appComponent.element<MyAppDeps>()
 * }
 * ```
 */
@Qualifier
annotation class ComponentElementBinding<C : Component>

@Given
fun <@Given T : @ComponentElementBinding<C> S, @ForTypeKey S, @ForTypeKey C : Component>
        componentElementBindingImpl(@Given factory: () -> T): ComponentElement<C> =
    typeKeyOf<S>() to factory as () -> Any?

/**
 * Will get invoked once [Component] [C] is initialized
 *
 * Example:
 * ```
 * @Given fun imageLoaderInitializer(@Given app: App): ComponentInitializer<AppComponent> = {
 *     ImageLoader.init(app)
 * }
 * ```
 */
typealias ComponentInitializer<C> = (C) -> Unit

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
        private val injectedElements: (Component) -> Set<ComponentElement<C>>,
        private val injectedInitializers: (Component) -> Set<ComponentInitializer<C>>
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
            val component = ComponentImpl(key, dependencies, elements, injectedElements) as C
            initializers?.forEach { it(component) }
            injectedInitializers(component).forEach { it(component) }
            return component
        }
    }
}
