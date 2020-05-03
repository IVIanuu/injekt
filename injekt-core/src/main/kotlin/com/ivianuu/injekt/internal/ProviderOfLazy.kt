package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.Provider

class ProviderOfLazy<T>(
    private val provider: Provider<T>
) : Provider<Lazy<T>> {
    override fun invoke(): Lazy<T> = DoubleCheck(provider)
}
