package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Provider

class KeyedProvider<T>(
    private val key: Int,
    private val component: Component
) : Provider<T> {
    override fun invoke(): T = component.get(key)
}
