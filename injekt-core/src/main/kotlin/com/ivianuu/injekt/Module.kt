package com.ivianuu.injekt

class Module @PublishedApi internal constructor(
    internal val bindings: Map<Key<*>, Binding<*>>?,
    internal val maps: Map<Key<*>, Map<*, Binding<*>>>?,
    internal val sets: Map<Key<*>, Map<Key<*>, Binding<*>>>?,
    internal val includes: Set<Module>?
)

class ModuleDsl {

    private var bindings: MutableMap<Key<*>, Binding<*>>? = null
    private var maps: MutableMap<Key<*>, MapDsl<*, *>>? = null
    private var sets: MutableMap<Key<*>, SetDsl<*>>? = null
    private var includes: MutableSet<Module>? = null

    /**
     * Registers the [binding] for [key]
     */
    fun <T> add(
        key: Key<T>,
        binding: Binding<T>
    ) {
        if (bindings != null) {
            check(key !in bindings!!) {
                "Already declared binding for $key"
            }
        } else bindings = mutableMapOf()
        bindings!![key] = binding
    }

    /**
     * Adds a map binding and runs the [block] in the scope of the [MapDsl] for [mapKey]
     */
    inline fun <K, V> map(
        mapKey: Key<Map<K, V>>,
        block: MapDsl<K, V>.() -> Unit = {}
    ) {
        getMapBuilder(mapKey).block()
    }

    /**
     * Adds a set binding and runs the [block] in the scope of the [SetDsl] for [setKey]
     */
    inline fun <E> set(
        setKey: Key<Set<E>>,
        block: SetDsl<E>.() -> Unit = {}
    ) {
        getSetBuilder(setKey).block()
    }

    fun include(module: Module) {
        if (includes != null) {
            check(module !in includes!!) {
                "Already included module $module"
            }
        } else includes = mutableSetOf()
        includes!! += module
    }

    @PublishedApi
    internal fun <K, V> getMapBuilder(mapKey: Key<Map<K, V>>): MapDsl<K, V> {
        var builder = maps?.get(mapKey) as? MapDsl<K, V>
        if (builder == null) {
            if (maps == null) maps = mutableMapOf()
            builder = MapDsl()
            maps!![mapKey] = builder
        }
        return builder
    }

    @PublishedApi
    internal fun <E> getSetBuilder(setKey: Key<Set<E>>): SetDsl<E> {
        var builder = sets?.get(setKey) as? SetDsl<E>
        if (builder == null) {
            if (sets == null) sets = mutableMapOf()
            builder = SetDsl()
            sets!![setKey] = builder
        }
        return builder
    }

    fun build(): Module = Module(
        bindings,
        maps?.mapValues { it.value.build() },
        sets?.mapValues { it.value.build() as Map<Key<*>, Binding<*>> },
        includes
    )

}

inline fun Module(block: ModuleDsl.() -> Unit = {}): Module =
    ModuleDsl().apply(block).build()
