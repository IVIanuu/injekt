package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.InjektPlugins
import com.ivianuu.injekt.isComponentExtensionRegistered
import com.ivianuu.injekt.registerComponentExtension

const val KEY_ORIGINAL_KEY = "original_key"

internal fun registerExtensionIfNeeded() {
    if (!InjektPlugins.isComponentExtensionRegistered(MultiBindingExtension)) {
        InjektPlugins.registerComponentExtension(MultiBindingExtension)
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