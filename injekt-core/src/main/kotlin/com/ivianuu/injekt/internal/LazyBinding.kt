package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Parameters

internal class LazyBinding<T>(
    private val component: Component,
    private val key: Key<T>
) : LinkedBinding<Lazy<T>>() {
    override fun invoke(parameters: Parameters): Lazy<T> = KeyedLazy(component, key)
}
