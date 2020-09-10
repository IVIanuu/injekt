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

inline fun rootContext(init: ContextBuilder.() -> Unit = {}): Context =
    ContextBuilder().apply(init).build()

inline fun Context.childContext(init: ContextBuilder.() -> Unit = {}): Context =
    ContextBuilder(this).apply(init).build()

class ContextBuilder(private val parent: Context? = null) {
    private val providers = mutableMapOf<Key<*>, @Reader () -> Any?>()
    @PublishedApi
    internal val mapBuilders = mutableMapOf<Key<*>, MapBuilder<*, *>>()
    @PublishedApi
    internal val setBuilders = mutableMapOf<Key<*>, SetBuilder<*>>()

    fun <T> given(key: Key<T>, provider: @Reader () -> T) {
        providers[key] = provider
    }

    inline fun <K, V> map(mapKey: Key<Map<K, V>>, block: MapBuilder<K, V>.() -> Unit) {
        mapBuilders.getOrPut(mapKey) { MapBuilder<K, V>(this) }
            .let { it as MapBuilder<K, V> }
            .block()
    }

    inline fun <E> set(setKey: Key<Set<E>>, block: SetBuilder<E>.() -> Unit) {
        setBuilders.getOrPut(setKey) { SetBuilder<E>(this) }
            .let { it as SetBuilder<E> }
            .block()
    }

    @Suppress("UNCHECKED_CAST")
    fun build(): Context {
        mapBuilders.forEach { (mapKey, mapBuilder) ->
            val keyedMapKey = KeyedMapKey(mapKey as Key<Map<Any?, Any?>>)
            val finalKeyedMap = mutableMapOf<Any?, Key<Any?>>()
            parent?.givenOrNull(keyedMapKey)?.let { finalKeyedMap += it }
            finalKeyedMap += mapBuilder.map as Map<Any?, Key<Any?>>
            providers[keyedMapKey] = { finalKeyedMap }
            providers[mapKey] = {
                finalKeyedMap
                    .mapValues { given(it.value) }
            }
        }
        setBuilders.forEach { (setKey, setBuilder) ->
            val keyedSetKey = KeyedSetKey(setKey as Key<Set<Any?>>)
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

inline fun <reified T> ContextBuilder.given(noinline provider: @Reader () -> T) {
    given(keyOf(), provider)
}

inline fun <reified K, reified V> ContextBuilder.map(block: MapBuilder<K, V>.() -> Unit) {
    map(keyOf(), block)
}

inline fun <reified E> ContextBuilder.set(block: SetBuilder<E>.() -> Unit) {
    set(keyOf(), block)
}

class MapBuilder<K, V>(private val contextBuilder: ContextBuilder) {
    internal val map = mutableMapOf<K, Key<out V>>()

    fun <T : V> put(entryKey: K, entryValueKey: Key<T>) {
        map[entryKey] = entryValueKey
    }

    inline fun <reified T : V> put(entryKey: K, noinline entryValueProvider: @Reader () -> T) {
        put(entryKey, keyOf(), entryValueProvider)
    }

    fun <T : V> put(entryKey: K, entryValueKey: Key<T>, entryValueProvider: @Reader () -> T) {
        map[entryKey] = entryValueKey
        contextBuilder.given(entryValueKey, entryValueProvider)
    }
}

internal data class KeyedMapKey<K, V>(val mapKey: Key<Map<K, V>>) : Key<Map<K, Key<V>>>

class SetBuilder<E>(private val contextBuilder: ContextBuilder) {
    internal val set = mutableSetOf<Key<out E>>()

    fun <T : E> add(elementKey: Key<T>) {
        set += elementKey
    }

    inline fun <reified T : E> add(noinline elementProvider: @Reader () -> T) {
        add(keyOf(), elementProvider)
    }

    fun <T : E> add(elementKey: Key<T>, elementProvider: @Reader () -> T) {
        set += elementKey
        contextBuilder.given(elementKey, elementProvider)
    }
}

internal data class KeyedSetKey<E>(val setkey: Key<Set<E>>) : Key<Set<Key<E>>>
