

package com.ivianuu.ast.utils

import kotlin.reflect.KClass

/**
 * [AttributeArrayOwner] based on different implementations of [ArrayMap] and switches them
 *   depending on array map fullness
 * [AttributeArrayOwner] can be used in classes with many instances,
 *   like user data for Fir elements or attributes for cone types
 *
 * Note that you can remove attributes from [AttributeArrayOwner] despite
 *   from components in [ComponentArrayOwner]
 */
@OptIn(Protected::class)
abstract class AttributeArrayOwner<K : Any, T : Any> : AbstractArrayMapOwner<K, T>() {
    @Suppress("UNCHECKED_CAST")
    final override var arrayMap: ArrayMap<T> = EmptyArrayMap as ArrayMap<T>
        private set

    final override fun registerComponent(tClass: KClass<out K>, value: T) {
        val id = typeRegistry.getId(tClass)
        when (arrayMap.size) {
            0 -> {
                arrayMap = OneElementArrayMap(value, id)
                return
            }

            1 -> {
                val map = arrayMap as OneElementArrayMap<T>
                if (map.index == id) {
                    arrayMap = OneElementArrayMap(value, id)
                    return
                } else {
                    arrayMap = ArrayMapImpl()
                    arrayMap[map.index] = map.value
                }
            }
        }

        arrayMap[id] = value
    }

    protected fun removeComponent(tClass: KClass<out K>) {
        val id = typeRegistry.getId(tClass)
        if (arrayMap[id] == null) return
        @Suppress("UNCHECKED_CAST")
        when (arrayMap.size) {
            1 -> arrayMap = EmptyArrayMap as ArrayMap<T>
            else -> {
                val map = arrayMap as ArrayMapImpl<T>
                map.remove(id)
                if (map.size == 1) {
                    val (index, value) = map.entries().first()
                    arrayMap = OneElementArrayMap(value, index)
                }
            }
        }
    }
}