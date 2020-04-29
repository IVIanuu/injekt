package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Provider

class InstanceProvider<T>(private val instance: T) : Provider<T> {
    override fun invoke() = instance
}
