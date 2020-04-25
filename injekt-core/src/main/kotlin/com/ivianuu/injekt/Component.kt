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

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

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
    val scope: KClass<*>,
    val parent: Component?,
    bindings: ConcurrentHashMap<Key<*>, Binding<*>>
) {

    val bindings: Map<Key<*>, Binding<*>> get() = _bindings
    private val _bindings = bindings

    private val linker = Linker(this)
    private val linkedKeys = mutableSetOf<Key<*>>()

    /**
     * Return a instance of type [T] for [key]
     */
    fun <T> get(key: Key<T>, parameters: Parameters = emptyParameters()): T {
        if (key is Key.ParameterizedKey && key.arguments.size == 1) {
            fun instanceKey() = keyOf<T>(
                classifier = key.arguments.single().classifier,
                qualifier = key.qualifier
            )
            when (key.arguments.single().classifier) {
                Lazy::class -> KeyedLazy(this, instanceKey())
                Provider::class -> KeyedProvider(linker, instanceKey())
            }
        }

        return linker.get(key)(parameters)
    }

    internal fun <T> getProvider(key: Key<T>): Provider<T> {
        findExistingProvider(key)?.let { return it }

        if (key.isNullable) return NullProvider as Provider<T>

        error("Couldn't find binding for $key")
    }

    private fun <T> findExistingProvider(key: Key<T>): Provider<T>? {
        _bindings[key]
            ?.provider
            ?.linkIfNeeded(key)
            ?.let { return it as? Provider<T> }

        return parent?.findExistingProvider(key)
    }

    private fun putJitBinding(key: Key<*>, binding: Binding<*>) {
        _bindings[key] = binding
    }

    private fun findComponent(scope: KClass<*>): Component? {
        if (this.scope == scope) return this
        return parent?.findComponent(scope)
    }

    private fun <T> Provider<T>.linkIfNeeded(key: Key<*>): Provider<T> {
        if (this is Linkable && linkedKeys.add(key)) link(linker)
        return this
    }

    private object NullProvider : Provider<Nothing?> {
        override fun invoke(parameters: Parameters) = null
    }

}

inline fun <reified T> Component.get(
    qualifier: KClass<*>? = null,
    parameters: Parameters = emptyParameters()
): T = get(keyOf(qualifier), parameters)

inline fun <reified T> Component.plus() = plus(T::class)

inline fun <reified T> Component.plus(vararg modules: Module) = plus(
    scope = T::class,
    modules = modules
)

fun Component.plus(scope: KClass<*>) = Component(scope = scope, parent = this)

fun Component.plus(scope: KClass<*>, vararg modules: Module) = Component(
    scope = scope,
    parent = this,
    modules = modules
)

inline fun <reified T> Component(
    vararg modules: Module,
    parent: Component? = null
) = Component(
    scope = T::class,
    modules = modules,
    parent = parent
)

fun Component(
    scope: KClass<*>,
    vararg modules: Module,
    parent: Component? = null
): Component {
    val bindings = mutableMapOf<Key<*>, Binding<*>>()

    modules.forEach { module ->
        module.bindings.forEach { (key, binding) ->
            if (binding.duplicateStrategy.check(
                    existsPredicate = { key in bindings },
                    errorMessage = { "Already declared binding for $key" })
            ) {
                bindings[key] = binding
            }
        }
    }

    if (parent != null) {
        val parentScopes = mutableListOf<KClass<*>>()

        var currentParent: Component? = parent
        while (currentParent != null) {
            parentScopes += currentParent.scope
            currentParent = currentParent.parent
        }

        check(scope !in parentScopes) {
            "Duplicated scope $scope"
        }
    }

    val finalBindings = ConcurrentHashMap(if (parent != null) {
        val parentBindings = parent.bindings
        val finalBindings = mutableMapOf<Key<*>, Binding<*>>()
        bindings.forEach { (key, binding) ->
            if (binding.duplicateStrategy.check(
                    existsPredicate = { key in parentBindings },
                    errorMessage = { "Already declared binding for $key" })
            ) {
                finalBindings[key] = binding
            }
        }
        finalBindings
    } else bindings)

    return Component(
        scope = scope,
        parent = parent,
        bindings = finalBindings
    )
}
