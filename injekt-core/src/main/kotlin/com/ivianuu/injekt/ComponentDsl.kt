package com.ivianuu.injekt

import com.ivianuu.injekt.internal.ComponentBinding
import com.ivianuu.injekt.internal.MapOfLazyBinding
import com.ivianuu.injekt.internal.MapOfProviderBinding
import com.ivianuu.injekt.internal.MapOfValueBinding
import com.ivianuu.injekt.internal.ModuleRegistry
import com.ivianuu.injekt.internal.SetOfLazyBinding
import com.ivianuu.injekt.internal.SetOfProviderBinding
import com.ivianuu.injekt.internal.SetOfValueBinding
import kotlin.reflect.KClass

class ComponentDsl(
    val scope: KClass<*>,
    val parent: Component?
) {

    private val bindings = mutableMapOf<Key<*>, Binding<*>>()
    private var maps: MutableMap<Key<*>, MapDsl<*, *>>? = null
    private var sets: MutableMap<Key<*>, SetDsl<*>>? = null

    init {
        ModuleRegistry.getForScope(scope).forEach { it() }
    }

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

    fun build(): Component {
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
                check(key !in parentBindings) {
                    "Already declared binding for $key"
                }
            }
        }

        bindings[keyOf<Component>()] = ComponentBinding
        bindings[keyOf<Component>(scope)] = ComponentBinding

        val mergedMaps = maps?.let { thisMaps ->
            thisMaps.mapValues { (mapKey, thisMap) ->
                val parentMap = parent?.maps?.get(mapKey)
                if (parentMap == null) {
                    thisMap.build()
                } else {
                    val mergedMap = MapDsl(parentMap) as MapDsl<Any?, Any?>
                    thisMap.build().forEach { (key, binding) ->
                        mergedMap.put(key, binding)
                    }
                    mergedMap.build()
                }
            }
        }

        mergedMaps?.forEach { (mapKey, map) ->
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

            bindings[mapOfProviderKey] = MapOfProviderBinding(map as Map<Any?, Binding<Any?>>)
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

        val mergedSets = sets?.let { thisSets ->
            thisSets.mapValues { (setKey, thisSet) ->
                val parentSet = parent?.sets?.get(setKey)
                if (parentSet == null) {
                    thisSet.build()
                } else {
                    val mergedSet = SetDsl(parentSet)
                    thisSet.build().forEach { (key, binding) ->
                        mergedSet.add(key as Key<Any?>, binding as Binding<Any?>)
                    }
                    mergedSet.build()
                }
            }
        }

        mergedSets?.forEach { (setKey, set) ->
            setKey as Key.ParameterizedKey
            val setOfProviderKey = Key.ParameterizedKey<Set<Provider<*>>>(
                classifier = Set::class,
                arguments = arrayOf(
                    Key.ParameterizedKey<Provider<*>>(
                        classifier = Provider::class,
                        arguments = arrayOf(setKey.arguments[0])
                    )
                ),
                qualifier = setKey.qualifier
            )

            bindings[setOfProviderKey] =
                SetOfProviderBinding(set.values.toSet() as Set<Binding<Any?>>)
            bindings[setKey] = SetOfValueBinding(setOfProviderKey as Key<Set<Provider<Any?>>>)
            bindings[Key.ParameterizedKey<Set<Lazy<*>>>(
                classifier = Set::class,
                arguments = arrayOf(
                    Key.ParameterizedKey<Lazy<*>>(
                        classifier = Lazy::class,
                        arguments = arrayOf(setKey.arguments[0])
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
            maps = mergedMaps,
            sets = mergedSets as? Map<Key<*>, Map<Key<*>, Binding<*>>>
        )
    }

}
