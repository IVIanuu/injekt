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

package com.ivianuu.injekt.scope

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.common.ForTypeKey
import com.ivianuu.injekt.common.TypeKey
import com.ivianuu.injekt.common.typeKeyOf

/**
 * A hierarchical construct with a lifecycle which hosts a map of keys to elements
 * which can be retrieved via [GivenScope.elementOrNull] or [GivenScope.element]
 */
interface GivenScope : GivenScopeDisposable {
    /**
     * Whether or not this scope is disposed
     */
    val isDisposed: Boolean
    /**
     * The exact type key of this scope
     */
    val key: TypeKey<GivenScope>

    /**
     * Returns the element [T] for [key] or null
     */
    fun <T> elementOrNull(key: TypeKey<T>): T?
    /**
     * Returns the value [T] for [key] or null
     */
    fun <T : Any> getScopedValueOrNull(key: Any): T?
    /**
     * Sets the value [T] for [key] to [value]
     */
    fun <T : Any> setScopedValue(key: Any, value: T)
    /**
     * Removes the value for [key]
     */
    fun removeScopedValue(key: Any)

    /**
     * Construct a [GivenScope] instance
     */
    interface Builder<S : GivenScope> {
        /**
         * Adds [parent] as a dependency
         */
        fun <T : GivenScope> dependency(parent: T): Builder<S>
        /**
         * Registers a element for [key] which will be provided by [factory]
         */
        fun <T> element(key: TypeKey<T>, factory: () -> T): Builder<S>
        /**
         * Registers the [initializer]
         */
        fun initializer(initializer: GivenScopeInitializer<S>): Builder<S>
        /**
         * Returns the configured [GivenScope] instance
         */
        fun build(): S
    }
}

/**
 * Allows scoped values to be notified when the hosting [Scope] get's disposed
 */
fun interface GivenScopeDisposable {
    /**
     * Get's called while the hosting [Scope] get's disposed via [Scope.dispose]
     */
    fun dispose()
}


/**
 * Returns the element [T] for [key] or throws
 */
fun <T> GivenScope.element(key: TypeKey<T>): T = elementOrNull(key)
    ?: error("No element for for $key in ${this.key}")

fun <@ForTypeKey T> GivenScope.element(): T =
    element(typeKeyOf())

/**
 * Returns an existing instance of [T] for key [key] or creates and caches a new instance by calling function [init]
 */
inline fun <T : Any> GivenScope.getOrCreateScopedValue(key: Any, init: () -> T): T {
    getScopedValueOrNull<T>(key)?.let { return it }
    synchronized(this) {
        getScopedValueOrNull<T>(key)?.let { return it }
        val value = init()
        setScopedValue(key, value)
        return value
    }
}

inline fun <@ForTypeKey T : Any> GivenScope.getOrCreateScopedValue(block: () -> T): T =
    getOrCreateScopedValue(typeKeyOf<T>(), block)

/**
 * Invokes the [action] function once [this] scope get's disposed
 * or invokes it synchronously if [this] is already disposed
 *
 * Returns a [GivenScopeDisposable] to unregister the [action]
 */
fun GivenScope.invokeOnDispose(action: () -> Unit): GivenScopeDisposable {
    if (isDisposed) {
        action()
        return NoOpScopeDisposable
    }
    synchronized(this) {
        if (isDisposed) {
            action()
            return NoOpScopeDisposable
        }
        val key = InvokeOnDisposeKey()
        var notifyDisposal = true
        setScopedValue(key, GivenScopeDisposable {
            if (notifyDisposal) action()
        })
        return GivenScopeDisposable {
            notifyDisposal = false
            removeScopedValue(key)
        }
    }
}

private class InvokeOnDisposeKey

private val NoOpScopeDisposable = GivenScopeDisposable {  }

@Given
fun <@ForTypeKey S : GivenScope> givenScopeBuilder(
    @Given elements: (@Given S) -> Set<GivenScopeElement<S>> = { emptySet() },
    @Given initializers: (@Given S) -> Set<GivenScopeInitializer<S>> = { emptySet() }
): GivenScope.Builder<S> = GivenScopeImpl.Builder(typeKeyOf<S>(), elements, initializers)

fun <S : GivenScope, @ForTypeKey T> GivenScope.Builder<S>.element(factory: () -> T) =
    element(typeKeyOf(), factory)

typealias GivenScopeElement<@Suppress("unused") C> = Pair<TypeKey<*>, () -> Any?>

/**
 * Registers the declaration a element in the [GivenScope] [S]
 *
 * Example:
 * ```
 * @GivenScopeElementBinding<AppGivenScope>
 * @Given
 * class MyAppDeps(@Given api: Api, @Given database: Database)
 *
 * fun runApp(@Given appScope: AppGivenScope) {
 *    val deps = appScope.element<MyAppDeps>()
 * }
 * ```
 */
@Qualifier
annotation class GivenScopeElementBinding<S : GivenScope>

@Given
fun <@Given T : @GivenScopeElementBinding<U> S, @ForTypeKey S, @ForTypeKey U : GivenScope>
        givenScopeElementBindingImpl(@Given factory: () -> T): GivenScopeElement<U> =
    typeKeyOf<S>() to factory as () -> Any?

/**
 * Will get invoked once [GivenScope] [S] is initialized
 *
 * Example:
 * ```
 * @Given fun imageLoaderInitializer(@Given app: App): GivenScopeInitializer<AppGivenScope> = {
 *     ImageLoader.init(app)
 * }
 * ```
 */
typealias GivenScopeInitializer<S> = (S) -> Unit

@PublishedApi
internal class GivenScopeImpl(
    override val key: TypeKey<GivenScope>,
    private val dependencies: List<GivenScope>?,
    explicitElements: Map<TypeKey<*>, () -> Any?>?,
    injectedElements: (GivenScope) -> Set<GivenScopeElement<*>>,
) : GivenScope {
    override var isDisposed = false

    private val elements = (explicitElements ?: emptyMap()) + injectedElements(this)
    private val values = mutableMapOf<Any, Any>()

    override fun <T> elementOrNull(key: TypeKey<T>): T? {
        if (key == this.key) return this as T
        elements[key]?.let { return it() as T }

        if (dependencies != null) {
            for (dependency in dependencies)
                dependency.elementOrNull(key)?.let { return it }
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getScopedValueOrNull(key: Any): T? {
        if (isDisposed) return null
        return values[key] as? T
    }

    override fun <T : Any> setScopedValue(key: Any, value: T) {
        if (isDisposed) return
        removeScopedValue(key)
        values[key] = value
    }

    override fun removeScopedValue(key: Any) {
        if (isDisposed) return
        removeImpl(key)
    }

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        if (values.isNotEmpty()) {
            values.keys
                .toList()
                .forEach { removeImpl(it) }
        }
    }

    private fun removeImpl(key: Any) {
        (values.remove(key) as? GivenScopeDisposable)?.dispose()
    }

    class Builder<S : GivenScope>(
        private val key: TypeKey<GivenScope>,
        private val injectedElements: (S) -> Set<GivenScopeElement<S>>,
        private val injectedInitializers: (S) -> Set<GivenScopeInitializer<S>>
    ) : GivenScope.Builder<S> {
        private var dependencies: MutableList<GivenScope>? = null
        private var elements: MutableMap<TypeKey<*>, () -> Any?>? = null
        private var initializers: MutableList<GivenScopeInitializer<S>>? = null

        override fun <T : GivenScope> dependency(parent: T): GivenScope.Builder<S> =
            apply {
                (dependencies ?: mutableListOf<GivenScope>()
                    .also { dependencies = it }) += parent
            }

        override fun <T> element(key: TypeKey<T>, factory: () -> T): GivenScope.Builder<S> =
            apply {
                (elements ?: mutableMapOf<TypeKey<*>, () -> Any?>()
                    .also { elements = it })[key] = factory
            }

        override fun initializer(initializer: GivenScopeInitializer<S>): GivenScope.Builder<S> =
            apply {
                (initializers ?: mutableListOf<GivenScopeInitializer<S>>()
                    .also { initializers = it }) += initializer
            }

        override fun build(): S {
            val givenScope = GivenScopeImpl(key, dependencies, elements,
                injectedElements as (GivenScope) -> Set<GivenScopeElement<Any>>) as S
            initializers?.forEach { it(givenScope) }
            injectedInitializers(givenScope).forEach { it(givenScope) }
            return givenScope
        }
    }
}
