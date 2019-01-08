package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.BeanDefinition
import com.ivianuu.injekt.Component

internal fun Component.getAllDefinitions(): Set<BeanDefinition<*>> =
    mutableSetOf<BeanDefinition<*>>().also { collectDefinitions(it) }

internal fun Component.collectDefinitions(
    definitions: MutableSet<BeanDefinition<*>>
) {
    definitions.addAll(getDefinitions())
    getDependencies().forEach { it.collectDefinitions(definitions) }
}