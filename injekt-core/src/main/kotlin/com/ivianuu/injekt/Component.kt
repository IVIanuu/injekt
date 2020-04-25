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
    val bindings: Map<Key<*>, Binding<*>>
) {

    internal val linker = Linker(this)

    inline fun <reified T> get(
        qualifier: KClass<*>? = null,
        parameters: Parameters = emptyParameters()
    ): T = get(keyOf(qualifier), parameters)

    /**
     * Return a instance of type [T] for [key]
     */
    fun <T> get(key: Key<T>, parameters: Parameters = emptyParameters()): T =
        linker.get(key)(parameters)

    fun plus(scope: KClass<*>, vararg modules: Module) = Component(
        scope = scope,
        parent = this,
        modules = modules
    )

}

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

    val finalBindings = if (parent != null) {
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
    } else bindings

    return Component(
        scope = scope,
        parent = parent,
        bindings = finalBindings
    )
}
