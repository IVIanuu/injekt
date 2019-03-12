package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.OverrideException
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.getOrDefault

const val KEY_ORIGINAL_KEY = "original_key"

internal fun Module.declareMapBinding(mapQualifier: Qualifier) {
    factory(qualifier = mapQualifier, override = true) {
        val allMapBindings = component.getAllBindings()
            .mapNotNull { binding ->
                binding.attributes.get<Map<Qualifier, MapBinding>>(KEY_MAP_BINDINGS)
                    ?.get(mapQualifier)?.let { binding to it }
            }

        val mapBindingsToUse = hashMapOf<Any, Binding<*>>()

        // check overrides
        allMapBindings.forEach { (binding, mapBinding) ->
            val isOverride = mapBindingsToUse.remove(mapBinding.key) != null

            if (isOverride && !mapBinding.override) {
                throw OverrideException("Try to override ${mapBinding.key} in map binding $mapBinding")
            }

            mapBindingsToUse[mapBinding.key] = binding
        }

        return@factory MultiBindingMap(component, mapBindingsToUse as Map<Any, Binding<Any>>)
    }
}

internal fun Module.declareSetBinding(setQualifier: Qualifier) {
    factory(qualifier = setQualifier, override = true) { _ ->
        val allSetBindings = component.getAllBindings()
            .mapNotNull { binding ->
                binding.attributes.get<Map<Qualifier, SetBinding>>(KEY_SET_BINDINGS)
                    ?.get(setQualifier)?.let { binding to it }
            }

        val setBindingsToUse = hashMapOf<Key, Binding<*>>()

        // check overrides
        allSetBindings.forEach { (binding, setBinding) ->
            val key = binding.attributes.getOrDefault(KEY_ORIGINAL_KEY, binding::key)

            val isOverride = setBindingsToUse.remove(binding.key) != null

            if (isOverride && !setBinding.override) {
                throw OverrideException("Try to override $key in set binding $setBinding")
            }

            setBindingsToUse[binding.key] = binding
        }

        return@factory MultiBindingSet(
            component,
            setBindingsToUse.values.toSet() as Set<Binding<Any>>
        )
    }

}

internal fun Component.getAllBindings(): Set<Binding<*>> =
    linkedSetOf<Binding<*>>().also(this::collectBindings)

internal fun Component.collectBindings(
    bindings: MutableSet<Binding<*>>
) {
    getDependencies().forEach { it.collectBindings(bindings) }
    bindings.addAll(getBindings())
}