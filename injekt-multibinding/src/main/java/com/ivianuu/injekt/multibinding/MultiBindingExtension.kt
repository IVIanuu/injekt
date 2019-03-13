package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentExtension
import com.ivianuu.injekt.FactoryKind
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.OverrideException
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.getOrDefault
import com.ivianuu.injekt.getOrSet

private const val KEY_REGISTERED_MAP_BINDINGS = "registered_map_bindings"
private const val KEY_REGISTERED_SET_BINDINGS = "registered_set_bindings"

/**
 * Extension which enables multi binding functionality
 * This extension will be automatically registered if you use multi binding
 */
object MultiBindingExtension : ComponentExtension {

    override fun onBindingAdded(component: Component, binding: Binding<*>) {
        super.onBindingAdded(component, binding)
        handleMap(component, binding)
        handleSet(component, binding)
    }

    private fun handleMap(component: Component, binding: Binding<*>) {
        val mapBindings =
            binding.attributes.get<Map<Qualifier, MapBinding>>(KEY_MAP_BINDINGS) ?: return
        mapBindings.forEach { addMapBinding(component, it.value) }
    }

    private fun addMapBinding(component: Component, mapBinding: MapBinding) {
        val registeredMapBindings = component.attributes
            .getOrSet<MutableSet<Qualifier>>(KEY_REGISTERED_MAP_BINDINGS) { hashSetOf() }

        if (registeredMapBindings.contains(mapBinding.mapQualifier)) return
        registeredMapBindings.add(mapBinding.mapQualifier)

        val multiMapBinding = Binding(
            qualifier = mapBinding.mapQualifier,
            kind = FactoryKind,
            definition = {
                val allMapBindings = component.getAllBindings()
                    .mapNotNull { binding ->
                        binding.attributes.get<Map<Qualifier, MapBinding>>(KEY_MAP_BINDINGS)
                            ?.get(mapBinding.mapQualifier)?.let { binding to it }
                    }

                val mapBindingsToUse = linkedMapOf<Any, Binding<*>>()

                // check overrides
                allMapBindings.forEach { (binding, mapBinding) ->
                    val isOverride = mapBindingsToUse.remove(mapBinding.key) != null

                    if (isOverride && !mapBinding.override) {
                        throw OverrideException("Try to override ${mapBinding.key} in map binding $mapBinding")
                    }

                    mapBindingsToUse[mapBinding.key] = binding
                }

                return@Binding MultiBindingMap(
                    component,
                    mapBindingsToUse as Map<Any, Binding<Any>>
                )
            }
        )

        component.addBinding(multiMapBinding)
    }

    private fun handleSet(component: Component, binding: Binding<*>) {
        val setBindings =
            binding.attributes.get<Map<Qualifier, SetBinding>>(KEY_SET_BINDINGS) ?: return
        setBindings.forEach { addSetBinding(component, it.value) }
    }

    private fun addSetBinding(component: Component, setBinding: SetBinding) {
        val registeredSetBindings = component.attributes
            .getOrSet<MutableSet<Qualifier>>(KEY_REGISTERED_SET_BINDINGS) { hashSetOf() }

        if (registeredSetBindings.contains(setBinding.setQualifier)) return
        registeredSetBindings.add(setBinding.setQualifier)

        val multiSetBinding = Binding(
            qualifier = setBinding.setQualifier,
            kind = FactoryKind,
            definition = {
                val allSetBindings = component.getAllBindings()
                    .mapNotNull { binding ->
                        binding.attributes.get<Map<Qualifier, SetBinding>>(KEY_SET_BINDINGS)
                            ?.get(setBinding.setQualifier)?.let { binding to it }
                    }

                val setBindingsToUse = linkedMapOf<Key, Binding<*>>()

                // check overrides
                allSetBindings.forEach { (binding, setBinding) ->
                    val key = binding.attributes.getOrDefault(KEY_ORIGINAL_KEY, binding::key)

                    val isOverride = setBindingsToUse.remove(binding.key) != null

                    if (isOverride && !setBinding.override) {
                        throw OverrideException("Try to override $key in set binding $setBinding")
                    }

                    setBindingsToUse[binding.key] = binding
                }

                return@Binding MultiBindingSet(
                    component,
                    setBindingsToUse.values.toSet() as Set<Binding<Any>>
                )
            }
        )

        component.addBinding(multiSetBinding)
    }
}