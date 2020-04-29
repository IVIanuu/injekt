package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Key
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.UnlinkedBinding

internal class AliasBinding<T>(private val originalKey: Key<T>) : UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> = linker.get(originalKey)
}
