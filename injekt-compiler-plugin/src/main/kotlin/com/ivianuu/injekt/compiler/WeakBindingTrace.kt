package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.com.intellij.util.keyFMap.KeyFMap
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import java.util.WeakHashMap

class WeakBindingTrace {
    private val map = WeakHashMap<Any, KeyFMap>()

    fun <K : IrAttributeContainer, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
        var holder = map[key.attributeOwnerId] ?: KeyFMap.EMPTY_MAP
        val prev = holder.get(slice.key)
        if (prev != null) {
            holder = holder.minus(slice.key)
        }
        holder = holder.plus(slice.key, value)
        map[key.attributeOwnerId] = holder
    }

    operator fun <K : IrAttributeContainer, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        return map[key.attributeOwnerId]?.get(slice.key)
    }
}

private val InjektTemporaryGlobalBindingTrace = WeakBindingTrace()

val IrGeneratorContext.irTrace: WeakBindingTrace get() = InjektTemporaryGlobalBindingTrace
