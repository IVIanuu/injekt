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

import com.ivianuu.injekt.internal.HasScope
import com.ivianuu.injekt.internal.JitBindingRegistry
import com.ivianuu.injekt.internal.LazyBinding
import com.ivianuu.injekt.internal.NullBinding
import com.ivianuu.injekt.internal.ProviderBinding
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
    val maps: Map<Key<*>, Map<*, Binding<*>>>?,
    val sets: Map<Key<*>, Map<Key<*>, Binding<*>>>?,
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

inline fun <reified T> Component.plus(noinline block: @Module () -> Unit = {}) =
    plus(T::class, block)

fun Component.plus(scope: KClass<*>, block: @Module () -> Unit = {}) = Component(
    scope = scope,
    parent = this,
    block = block
)

inline fun <reified T> Component(
    parent: Component? = null,
    noinline block: @Module () -> Unit = {}
) = Component(
    scope = T::class,
    parent = parent,
    block = block
)

@JvmName("DefaultComponent")
fun Component(block: @Module () -> Unit = {}): Component = Component(
    scope = ApplicationScoped::class,
    block = block
)


fun Component(
    scope: KClass<*>,
    parent: Component? = null,
    block: @Module () -> Unit = {}
): Component = ComponentDsl(scope, parent).apply(block as (ComponentDsl) -> Unit).build()
