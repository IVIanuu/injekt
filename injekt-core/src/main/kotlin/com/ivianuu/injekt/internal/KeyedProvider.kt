package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider

internal class KeyedProvider<T>(
    private val linker: Linker,
    private val key: Key<T>
) : Provider<T> {

    private var provider: Provider<T>? = null

    override fun invoke(parameters: Parameters): T {
        if (provider == null) {
            provider = linker.get(key)
        }
        return provider!!(parameters)
    }

}
