

package com.ivianuu.ast.utils

import kotlin.reflect.KClass

/**
 * [ComponentArrayOwner] based on [ArrayMap] with flexible size and should be used for
 *   storing services in entities with limited number of instances, like FirSession
 */
@OptIn(Protected::class)
abstract class ComponentArrayOwner<K : Any, V : Any> : AbstractArrayMapOwner<K, V>() {
    final override val arrayMap: ArrayMap<V> =
        ArrayMapImpl()

    final override fun registerComponent(tClass: KClass<out K>, value: V) {
        arrayMap[typeRegistry.getId(tClass)] = value
    }

    protected operator fun get(key: KClass<out K>): V {
        val id = typeRegistry.getId(key)
        return arrayMap[id] ?: error("No '$key'($id) component in array: $this")
    }
}