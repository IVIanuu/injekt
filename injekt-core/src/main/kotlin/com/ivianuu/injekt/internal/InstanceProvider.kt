package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Provider

class InstanceProvider<T>(val instance: T) : Provider<T> {
    override fun invoke(): T = instance
}
