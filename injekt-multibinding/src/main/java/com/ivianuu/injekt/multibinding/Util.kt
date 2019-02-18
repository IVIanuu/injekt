package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component

const val KEY_ORIGINAL_KEY = "original_key"

internal fun Component.getAllBindings(): Set<Binding<*>> =
    mutableSetOf<Binding<*>>().also { collectBindings(it) }

internal fun Component.collectBindings(
    bindings: MutableSet<Binding<*>>
) {
    getDependencies().forEach { it.collectBindings(bindings) }
    bindings.addAll(getBindings())
}