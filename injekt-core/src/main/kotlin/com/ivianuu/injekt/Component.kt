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
 * The heart of the library which provides instances
 * Instances can be requested by calling [get]
 * Use [ComponentBuilder] to construct [Component] instances
 *
 * Typical usage of a [Component] looks like this:
 *
 * ´´´
 * val component = Component {
 *     single { Api(get()) }
 *     single { Database(get(), get()) }
 * }
 *
 * val api = component.get<Api>()
 * val database = component.get<Database>()
 * ´´´
 *
 * @see get
 * @see getLazy
 * @see ComponentBuilder
 */
class Component internal constructor(
    val scopes: Set<Scope>,
    val parents: List<Component>,
    val jitFactories: List<(Component, Key<Any?>) -> BindingProvider<Any?>?>,
    val bindings: Map<Key<*>, Binding<*>>
) {

    /**
     * Returns the binding for [key]
     */
    fun <T> getBindingProvider(key: Key<T>): BindingProvider<T> {
        findBindingProvider(key)?.let { return it }
        if (key.isNullable) return NullBindingProvider as BindingProvider<T>
        error("Couldn't get instance for $key")
    }

    /**
     * Returns the [Component] for [scope] or throws
     */
    fun getComponent(scope: Scope): Component =
        findComponent(scope) ?: error("Couldn't find component for scope $scope")

    private fun findComponent(scope: Scope): Component? {
        if (scope in scopes) return this

        for (index in parents.indices) {
            parents[index].findComponent(scope)?.let { return it }
        }

        return null
    }

    private fun <T> findBindingProvider(key: Key<T>): BindingProvider<T>? {
        (bindings[key] as? Binding<T>)
            ?.takeUnless { !key.isNullable && it.key.isNullable }
            ?.provider
            ?.let { return it }

        for (index in parents.lastIndex downTo 0) {
            parents[index].findBindingProvider(key)?.let { return it }
        }

        for (index in jitFactories.lastIndex downTo 0) {
            (jitFactories[index](this, key as Key<Any?>) as? BindingProvider<T>)
                ?.let { return it }
        }

        return null
    }

    private companion object {
        private val NullBindingProvider: BindingProvider<Any?> = { null }
    }
}

inline fun <reified T> Component.get(
    qualifier: Qualifier = Qualifier.None,
    parameters: Parameters = emptyParameters()
): T =
    get(keyOf(qualifier), parameters)

/**
 * Return a instance of type [T] for [key]
 */
fun <T> Component.get(key: Key<T>, parameters: Parameters = emptyParameters()): T =
    getBindingProvider(key)(parameters)
