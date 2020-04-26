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

import com.ivianuu.injekt.internal.InstanceBinding
import com.ivianuu.injekt.internal.JitBindingRegistry
import com.ivianuu.injekt.internal.KeyedLazy
import com.ivianuu.injekt.internal.KeyedProvider
import com.ivianuu.injekt.internal.NullBinding
import com.ivianuu.injekt.internal.injektIntrinsic
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
    bindings: MutableMap<Key<*>, Binding<*>>
) {

    val bindings: Map<Key<*>, Binding<*>> get() = _bindings
    private val _bindings = bindings

    private val linker = Linker(this)

    /**
     * Return a instance of type [T] for [key]
     */
    fun <T> get(key: Key<T>, parameters: Parameters = emptyParameters()): T =
        linker.get(key)(parameters)

    internal fun <T> getBinding(key: Key<T>): LinkedBinding<T> {
        if (key is Key.ParameterizedKey && key.arguments.size == 1) {
            fun instanceKey() = keyOf<T>(
                classifier = key.arguments.single().classifier,
                qualifier = key.qualifier
            )
            when (key.arguments.single().classifier) {
                Lazy::class -> InstanceBinding(KeyedLazy(this, instanceKey()))
                Provider::class -> InstanceBinding(KeyedProvider(linker, instanceKey()))
            }
        }

        findExistingBinding(key)?.let { return it }

        val jitLookup = JitBindingRegistry.find(key)
        if (jitLookup != null) {
            val componentForBinding = findComponent(jitLookup.scope)
                ?: error("Incompatible binding with scope ${jitLookup.scope}")
            return componentForBinding.putJitBinding(key, jitLookup.binding)
        }

        if (key.isNullable) return NullBinding as LinkedBinding<T>

        error("Couldn't find binding for $key")
    }

    private fun <T> findExistingBinding(key: Key<T>): LinkedBinding<T>? {
        synchronized(_bindings) { _bindings[key] }
            ?.linkIfNeeded(key)
            ?.let { return it as? LinkedBinding<T> }

        return parent?.findExistingBinding(key)
    }

    private fun <T> putJitBinding(key: Key<T>, binding: Binding<T>): LinkedBinding<T> {
        val linked = binding.link(linker)
        synchronized(_bindings) { _bindings[key] = linked }
        return linked
    }

    private fun findComponent(scope: KClass<*>): Component? {
        if (this.scope == scope || Factory::class == scope) return this
        return parent?.findComponent(scope)
    }

    private fun <T> Binding<T>.linkIfNeeded(key: Key<*>): LinkedBinding<T> {
        if (this is LinkedBinding) return this
        val linked = link(linker)
        synchronized(_bindings) { _bindings[key] = linked }
        return linked
    }

}

inline fun <reified T> Component.get(
    qualifier: KClass<*>? = null,
    parameters: Parameters = emptyParameters()
): T = injektIntrinsic()

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
    modules = *modules,
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
            if (key in bindings) {
                error("Already declared binding for $key")
            }
            bindings[key] = binding
        }
    }

    if (parent != null) {
        val parentScopes = mutableListOf<KClass<*>>()
        val parentBindings = mutableMapOf<Key<*>, Binding<*>>()

        var currentParent: Component? = parent
        while (currentParent != null) {
            parentScopes += currentParent.scope
            parentBindings += currentParent.bindings
            currentParent = currentParent.parent
        }

        check(scope !in parentScopes) {
            "Duplicated scope $scope"
        }

        bindings.forEach { (key, _) ->
            if (key in parentBindings) {
                error("Already declared binding for $key")
            }
        }
    }

    return Component(
        scope = scope,
        parent = parent,
        bindings = bindings
    )
}
