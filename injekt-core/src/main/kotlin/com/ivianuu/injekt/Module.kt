package com.ivianuu.injekt

class Module @PublishedApi internal constructor(
    internal val bindings: Map<Key<*>, Binding<*>>,
    internal val maps: Map<Key<*>, Map<*, Key<*>>>?,
    internal val sets: Map<Key<*>, Set<Key<*>>>?
)

class ModuleDsl {

    private val bindings = mutableMapOf<Key<*>, Binding<*>>()
    private var mapBuilders: MutableMap<Key<*>, MapBuilder<*, *>>? = null
    private var setBuilders: MutableMap<Key<*>, SetBuilder<*>>? = null

    /**
     * Registers the [binding] for [key]
     */
    fun <T> add(
        key: Key<T>,
        binding: Binding<T>
    ) {
        check(key !in bindings) {
            "Already declared binding for $key"
        }
        bindings[key] = binding
    }

    /**
     * Adds a map binding and runs the [block] in the scope of the [MapBuilder] for [mapKey]
     */
    inline fun <K, V> map(
        mapKey: Key<Map<K, V>>,
        block: MapBuilder<K, V>.() -> Unit = {}
    ) {
        getMapBuilder(mapKey).block()
    }

    /**
     * Adds a set binding and runs the [block] in the scope of the [SetBuilder] for [setKey]
     */
    inline fun <E> set(
        setKey: Key<Set<E>>,
        block: SetBuilder<E>.() -> Unit = {}
    ) {
        getSetBuilder(setKey).block()
    }

    @PublishedApi
    internal fun <K, V> getMapBuilder(mapKey: Key<Map<K, V>>): MapBuilder<K, V> {
        var builder = mapBuilders?.get(mapKey) as? MapBuilder<K, V>
        if (builder == null) {
            if (mapBuilders == null) mapBuilders = mutableMapOf()
            builder = MapBuilder()
            mapBuilders!![mapKey] = builder
        }
        return builder
    }

    @PublishedApi
    internal fun <E> getSetBuilder(setKey: Key<Set<E>>): SetBuilder<E> {
        var builder = setBuilders?.get(setKey) as? SetBuilder<E>
        if (builder == null) {
            if (setBuilders == null) setBuilders = mutableMapOf()
            builder = SetBuilder()
            setBuilders!![setKey] = builder
        }
        return builder
    }

    fun build(): Module = Module(
        bindings,
        mapBuilders?.mapValues { it.value.build() },
        setBuilders?.mapValues { it.value.build() }
    )

}

fun Module(block: ModuleDsl.() -> Unit = {}): Module =
    ModuleDsl().apply(block).build()
