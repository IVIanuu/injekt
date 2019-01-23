package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.OverrideException
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.getOrDefault

const val KEY_ORIGINAL_KEY = "original_key"

internal fun Module.declareMapBinding(mapName: String) {
    factory(name = mapName, override = true) { _ ->
        val allMapBindings = component.getAllBindings()
            .mapNotNull { binding ->
                binding.attributes.get<Map<String, MapBinding>>(KEY_MAP_BINDINGS)
                    ?.get(mapName)?.let { binding to it }
            }

        val existingKeys = mutableSetOf<Any>()

        // check overrides
        allMapBindings.forEach { (_, mapBinding) ->
            if (!existingKeys.add(mapBinding.key) && !mapBinding.override) {
                throw OverrideException("Try to override ${mapBinding.key} in map binding $mapBinding")
            }
        }

        allMapBindings
            .map { it.second.key to it.first }
            .toMap()
            .mapValues { it.value as Binding<Any> }
            .let { MultiBindingMap(component, it) }
    }
}

internal fun Module.declareSetBinding(setName: String) {
    factory(name = setName, override = true) { _ ->
        val allSetBindings = component.getAllBindings()
            .mapNotNull { binding ->
                binding.attributes.get<Map<String, SetBinding>>(KEY_SET_BINDINGS)
                    ?.get(setName)?.let { binding to it }
            }

        val existingKeys = mutableSetOf<Key>()

        // check overrides
        allSetBindings.forEach { (binding, setBinding) ->
            val key = binding.attributes.getOrDefault(KEY_ORIGINAL_KEY) { binding.key }
            if (!existingKeys.add(key) && !setBinding.override) {
                throw OverrideException("Try to override $key in set binding $setBinding")
            }
        }

        allSetBindings
            .map { it.first as Binding<Any> }
            .toSet()
            .let { MultiBindingSet(component, it) }
    }

}

internal fun Component.getAllBindings(): Set<Binding<*>> =
    mutableSetOf<Binding<*>>().also { collectBindings(it) }

internal fun Component.collectBindings(
    bindings: MutableSet<Binding<*>>
) {
    getDependencies().forEach { it.collectBindings(bindings) }
    bindings.addAll(getBindings())
}