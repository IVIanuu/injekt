package com.ivianuu.injekt

import com.ivianuu.injekt.internal.ComponentBinding
import com.ivianuu.injekt.internal.MapOfLazyBinding
import com.ivianuu.injekt.internal.MapOfProviderBinding
import com.ivianuu.injekt.internal.MapOfValueBinding
import com.ivianuu.injekt.internal.ModuleRegistry
import com.ivianuu.injekt.internal.SetOfLazyBinding
import com.ivianuu.injekt.internal.SetOfProviderBinding
import com.ivianuu.injekt.internal.SetOfValueBinding
import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

class ComponentDsl(
    val scope: KClass<*>,
    val parent: Component?
) {

    internal val bindings = mutableMapOf<Key<*>, Binding<*>>()
    internal var maps: MutableMap<Key<*>, MapDsl<*, *>>? = null
    internal var sets: MutableMap<Key<*>, SetDsl<*>>? = null

    init {
        ModuleRegistry.getForScope(scope).forEach {
            (it as (ComponentDsl) -> Unit)(this)
        }
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
        bindings[keyOf<Component>(qualifier = scope)] = ComponentBinding

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
            val mapOfProviderKey = parameterizedKeyOf<Map<*, Provider<*>>>(
                classifier = Map::class,
                arguments = arrayOf(
                    mapKey.arguments[0],
                    parameterizedKeyOf<Provider<*>>(
                        classifier = Provider::class,
                        arguments = arrayOf(mapKey.arguments[1])
                    )
                ),
                qualifier = mapKey.qualifier
            )

            bindings[mapOfProviderKey] = MapOfProviderBinding(map as Map<Any?, Binding<Any?>>)
            bindings[mapKey] = MapOfValueBinding(mapOfProviderKey as Key<Map<Any?, Provider<Any?>>>)
            bindings[parameterizedKeyOf<Map<*, Lazy<*>>>(
                classifier = Map::class,
                arguments = arrayOf(
                    mapKey.arguments[0],
                    parameterizedKeyOf<Lazy<*>>(
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
            val setOfProviderKey = parameterizedKeyOf<Set<Provider<*>>>(
                classifier = Set::class,
                arguments = arrayOf(
                    parameterizedKeyOf<Provider<*>>(
                        classifier = Provider::class,
                        arguments = arrayOf(setKey.arguments[0])
                    )
                ),
                qualifier = setKey.qualifier
            )

            bindings[setOfProviderKey] =
                SetOfProviderBinding(set.values.toSet() as Set<Binding<Any?>>)
            bindings[setKey] = SetOfValueBinding(setOfProviderKey as Key<Set<Provider<Any?>>>)
            bindings[parameterizedKeyOf<Set<Lazy<*>>>(
                classifier = Set::class,
                arguments = arrayOf(
                    parameterizedKeyOf<Lazy<*>>(
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

@Module
val componentDsl: ComponentDsl
    get() = injektIntrinsic()
