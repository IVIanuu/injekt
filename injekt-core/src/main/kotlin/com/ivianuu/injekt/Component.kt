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

import com.ivianuu.injekt.internal.ComponentBinding
import com.ivianuu.injekt.internal.HasScope
import com.ivianuu.injekt.internal.JitBindingRegistry
import com.ivianuu.injekt.internal.LazyBinding
import com.ivianuu.injekt.internal.MapOfLazyBinding
import com.ivianuu.injekt.internal.MapOfProviderBinding
import com.ivianuu.injekt.internal.MapOfValueBinding
import com.ivianuu.injekt.internal.NullBinding
import com.ivianuu.injekt.internal.ProviderBinding
import com.ivianuu.injekt.internal.SetOfLazyBinding
import com.ivianuu.injekt.internal.SetOfProviderBinding
import com.ivianuu.injekt.internal.SetOfValueBinding
import com.ivianuu.injekt.internal.asScoped
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
    bindings: MutableMap<Key<*>, Binding<*>>,
    val maps: Map<Key<*>, Map<*, Key<*>>>?,
    val sets: Map<Key<*>, Set<Key<*>>>?,
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
            fun instanceKey() = key.arguments.single().copy(qualifier = key.qualifier)
            when (key.classifier) {
                Lazy::class -> return LazyBinding(this, instanceKey()) as LinkedBinding<T>
                Provider::class -> return ProviderBinding(linker, instanceKey()) as LinkedBinding<T>
            }
        }

        findExistingBinding(key)?.let { return it }

        val jitBinding = JitBindingRegistry.find(key)
        if (jitBinding != null) {
            return if (jitBinding is HasScope) {
                val componentForBinding = findComponent(jitBinding.scope)
                    ?: error("Incompatible binding with scope ${jitBinding.scope}")
                componentForBinding.putJitBinding(key, jitBinding.asScoped())
            } else {
                putJitBinding(key, jitBinding)
            }
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
        if (this.scope == scope) return this
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
    modules = *modules
)

fun Component.plus(scope: KClass<*>) = Component(scope = scope, parent = this)

fun Component.plus(scope: KClass<*>, vararg modules: Module) = Component(
    scope = scope,
    parent = this,
    modules = *modules
)

inline fun <reified T> Component(
    vararg modules: Module,
    parent: Component? = null
) = Component(
    scope = T::class,
    modules = *modules,
    parent = parent
)

@JvmName("DefaultComponent")
fun Component(
    vararg modules: Module,
    parent: Component? = null
) = Component(
    scope = ApplicationScoped::class,
    modules = *modules,
    parent = parent
)

fun Component(
    scope: KClass<*>,
    vararg modules: Module,
    parent: Component? = null
): Component {
    val bindings = mutableMapOf<Key<*>, Binding<*>>()
    var maps: MutableMap<Key<*>, MutableMap<*, Key<*>>>? = parent?.maps
        ?.mapValues { it.value.toMutableMap() }
        ?.toMutableMap()
    var sets: MutableMap<Key<*>, MutableSet<Key<*>>>? = parent?.sets
        ?.mapValues { it.value.toMutableSet() }
        ?.toMutableMap()

    modules.forEach { module ->
        module.bindings.forEach { (key, binding) ->
            if (key in bindings) {
                error("Already declared binding for $key")
            }
            bindings[key] = binding
        }
        module.maps?.forEach { (mapKey, moduleMap) ->
            if (maps == null) maps = mutableMapOf()
            val thisMap =
                maps!!.getOrPut(mapKey) { mutableMapOf<Any?, Key<*>>() } as MutableMap<Any?, Key<*>>
            moduleMap.forEach { (key, valueKey) ->
                if (key in thisMap) {
                    error("Already declared $key")
                }
                thisMap[key] = valueKey
            }
        }
        module.sets?.forEach { (setKey, moduleSet) ->
            if (sets == null) sets = mutableMapOf()
            val thisSet = sets!!.getOrPut(setKey) { mutableSetOf() }
            moduleSet.forEach { elementKey ->
                if (elementKey in thisSet) {
                    error("Already declared $elementKey")
                }
                thisSet += elementKey
            }
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

    bindings[keyOf<Component>()] = ComponentBinding
    bindings[keyOf<Component>(scope)] = ComponentBinding

    maps?.forEach { (mapKey, map) ->
        mapKey as Key.ParameterizedKey
        val mapOfProviderKey = Key.ParameterizedKey<Map<*, Provider<*>>>(
            classifier = Map::class,
            arguments = arrayOf(
                mapKey.arguments[0],
                Key.ParameterizedKey<Provider<*>>(
                    classifier = Provider::class,
                    arguments = arrayOf(mapKey.arguments[1])
                )
            ),
            qualifier = mapKey.qualifier
        )

        bindings[mapOfProviderKey] = MapOfProviderBinding(map as Map<Any?, Key<Any?>>)
        bindings[mapKey] = MapOfValueBinding(mapOfProviderKey as Key<Map<Any?, Provider<Any?>>>)
        bindings[Key.ParameterizedKey<Map<*, Lazy<*>>>(
            classifier = Map::class,
            arguments = arrayOf(
                mapKey.arguments[0],
                Key.ParameterizedKey<Lazy<*>>(
                    classifier = Lazy::class,
                    arguments = arrayOf(mapKey.arguments[1])
                )
            ),
            qualifier = mapKey.qualifier
        )
        ] = MapOfLazyBinding(mapOfProviderKey as Key<Map<Any?, Provider<Any?>>>)
    }

    sets?.forEach { (setKey, set) ->
        setKey as Key.ParameterizedKey
        val setOfProviderKey = Key.ParameterizedKey<Set<Provider<*>>>(
            classifier = Set::class,
            arguments = arrayOf(
                setKey.arguments[0],
                Key.ParameterizedKey<Provider<*>>(
                    classifier = Provider::class,
                    arguments = arrayOf(setKey.arguments[1])
                )
            ),
            qualifier = setKey.qualifier
        )

        bindings[setOfProviderKey] = SetOfProviderBinding(set as Set<Key<Any?>>)
        bindings[setKey] = SetOfValueBinding(setOfProviderKey as Key<Set<Provider<Any?>>>)
        bindings[Key.ParameterizedKey<Set<Lazy<*>>>(
            classifier = Map::class,
            arguments = arrayOf(
                setKey.arguments[0],
                Key.ParameterizedKey<Lazy<*>>(
                    classifier = Lazy::class,
                    arguments = arrayOf(setKey.arguments[1])
                )
            ),
            qualifier = setKey.qualifier
        )
        ] = SetOfLazyBinding(setOfProviderKey as Key<Set<Provider<Any?>>>)
    }

    return Component(
        scope = scope,
        parent = parent,
        bindings = bindings,
        maps = maps,
        sets = sets
    )
}
