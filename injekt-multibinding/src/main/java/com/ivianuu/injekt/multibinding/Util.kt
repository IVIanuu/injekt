package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component

const val KEY_ORIGINAL_KEY = "originalKey"

internal fun Component.getAllBindings(): Set<Binding<*>> =
    mutableSetOf<Binding<*>>().also { collectBindings(it) }

internal fun Component.collectBindings(
    bindings: MutableSet<Binding<*>>
) {
    bindings.addAll(getBindings())
    getDependencies().forEach { it.collectBindings(bindings) }
}