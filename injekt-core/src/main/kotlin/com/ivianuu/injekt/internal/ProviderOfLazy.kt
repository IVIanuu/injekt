package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Lazy

class ProviderOfLazy<T>(private val provider: () -> T) : () -> @Lazy () -> T {
    override fun invoke() = DoubleCheck(provider)
}
