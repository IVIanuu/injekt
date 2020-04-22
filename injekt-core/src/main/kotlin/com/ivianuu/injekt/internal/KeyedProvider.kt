package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Provider

class KeyedProvider<T>(
    private val component: Component,
    private val key: Int
) : Provider<T> {
    override fun invoke(): T = component.get(key)
}
