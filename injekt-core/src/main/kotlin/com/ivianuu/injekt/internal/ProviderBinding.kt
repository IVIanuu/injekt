package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Key
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider

internal class ProviderBinding<T>(
    private val linker: Linker,
    private val key: Key<T>
) : LinkedBinding<Provider<T>>() {
    override fun invoke(parameters: Parameters): Provider<T> = KeyedProvider(linker, key)
}