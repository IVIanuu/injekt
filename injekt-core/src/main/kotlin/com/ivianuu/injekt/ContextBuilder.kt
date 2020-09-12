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

package com.ivianuu.injekt

inline fun rootContext(
    init: ContextBuilder.() -> Unit = {}
): Context = ContextBuilder().apply(init).build()

@JvmName("rootContextT")
inline fun <@ForKey T : ContextName> rootContext(
    init: ContextBuilder.() -> Unit = {}
): Context = rootContext(keyOf<T>(), init)

inline fun rootContext(
    contextName: Key<ContextName>,
    init: ContextBuilder.() -> Unit = {}
): Context = ContextBuilder(contextName).apply(init).build()

inline fun Context.childContext(
    init: ContextBuilder.() -> Unit = {}
): Context = ContextBuilder(null, this).apply(init).build()

@JvmName("childContextT")
inline fun <@ForKey T : ContextName> Context.childContext(
    init: ContextBuilder.() -> Unit = {}
): Context = childContext(keyOf<T>(), init)

inline fun Context.childContext(
    contextName: Key<ContextName>,
    init: ContextBuilder.() -> Unit = {}
): Context = ContextBuilder(contextName, this).apply(init).build()

@Reader
inline fun childContext(
    init: ContextBuilder.() -> Unit = {}
): Context = currentContext.childContext(init)

@JvmName("childContextTyped")
@Reader
inline fun <@ForKey T : ContextName> childContext(
    init: ContextBuilder.() -> Unit = {}
): Context = currentContext.childContext(init)

@Reader
inline fun childContext(
    contextName: Key<ContextName>,
    init: ContextBuilder.() -> Unit = {}
): Context = currentContext.childContext(contextName, init)

class ContextBuilder(
    val contextName: Key<ContextName>? = null,
    val parent: Context? = null
) {
    private val providers = mutableMapOf<Key<*>, @Reader () -> Any?>()

    private var mapBuilders: MutableMap<Key<*>, MapBuilder<*, *>>? = null

    @PublishedApi
    internal fun mapBuilders() = mapBuilders ?: mutableMapOf<Key<*>, MapBuilder<*, *>>()
        .also { mapBuilders = it }

    private var setBuilders: MutableMap<Key<*>, SetBuilder<*>>? = null

    @PublishedApi
    internal fun setBuilders() = setBuilders ?: mutableMapOf<Key<*>, SetBuilder<*>>()
        .also { setBuilders = it }

    init {
        ModuleRegistry._modules[keyOf<AnyContext>()]?.forEach { it() }
        if (contextName != null) {
            ModuleRegistry._modules[contextName]?.forEach { it() }
        }
    }

    fun <@ForKey T> unscoped(
        key: Key<T> = keyOf(),
        duplicatePolicy: DuplicatePolicy = DuplicatePolicy.Fail,
        provider: @Reader () -> T
    ) {
        duplicatePolicy.check(
            existsPredicate = { key in providers },
            errorMessage = { "Already specified given for '$key'" }
        )
        providers[key] = provider
    }

    inline fun <@ForKey K, @ForKey V> map(
        mapKey: Key<Map<K, V>> = keyOf(),
        block: MapBuilder<K, V>.() -> Unit
    ) {
        mapBuilders().getOrPut(mapKey) { MapBuilder<K, V>(this) }
            .let { it as MapBuilder<K, V> }
            .block()
    }

    inline fun <@ForKey E> set(setKey: Key<Set<E>> = keyOf(), block: SetBuilder<E>.() -> Unit) {
        setBuilders().getOrPut(setKey) { SetBuilder<E>(this) }
            .let { it as SetBuilder<E> }
            .block()
    }

    fun build(): Context {
        mapBuilders?.forEach { (mapKey, mapBuilder) ->
            val keyedMapKey = Key<Map<Any?, Key<Any?>>>("${mapKey.value}.keys")
            val finalKeyedMap = mutableMapOf<Any?, Key<Any?>>()
            parent?.givenOrNull(keyedMapKey)?.let { finalKeyedMap += it }
            finalKeyedMap += mapBuilder.map as Map<Any?, Key<Any?>>
            providers[keyedMapKey] = { finalKeyedMap }
            providers[mapKey] = {
                finalKeyedMap
                    .mapValues { given(it.value) }
            }
        }
        setBuilders?.forEach { (setKey, setBuilder) ->
            val keyedSetKey = Key<Set<Key<Any?>>>("${setKey.value}.keys")
            val finalKeyedSet = mutableSetOf<Key<Any?>>()
            parent?.givenOrNull(keyedSetKey)?.let { finalKeyedSet += it }
            finalKeyedSet += setBuilder.set as Set<Key<Any?>>
            providers[keyedSetKey] = { finalKeyedSet }
            providers[setKey] = {
                finalKeyedSet
                    .mapTo(mutableSetOf()) { given(it) }
            }
        }
        return ContextImpl(parent, providers)
    }
}

class MapBuilder<K, V>(private val contextBuilder: ContextBuilder) {
    internal val map = mutableMapOf<K, Key<V>>()

    fun <@ForKey T : V> put(
        entryKey: K,
        entryValueKey: Key<T> = keyOf(),
        duplicatePolicy: DuplicatePolicy = DuplicatePolicy.Fail,
        entryValueProvider: @Reader (() -> T)? = null
    ) {
        duplicatePolicy.check(
            existsPredicate = { entryKey in map },
            errorMessage = { "Already specified map entry for '$entryKey'" }
        )
        map[entryKey] = entryValueKey
        if (entryValueProvider != null)
            contextBuilder.unscoped(entryValueKey, duplicatePolicy, entryValueProvider)
    }
}

class SetBuilder<E>(private val contextBuilder: ContextBuilder) {
    internal val set = mutableSetOf<Key<E>>()

    fun <@ForKey T : E> add(
        elementKey: Key<T> = keyOf(),
        duplicatePolicy: DuplicatePolicy = DuplicatePolicy.Fail,
        elementProvider: @Reader (() -> T)? = null
    ) {
        duplicatePolicy.check(
            existsPredicate = { elementKey in set },
            errorMessage = { "Already specified map entry for '$elementKey'" }
        )
        set += elementKey
        if (elementProvider != null)
            contextBuilder.unscoped(elementKey, duplicatePolicy, elementProvider)
    }
}
