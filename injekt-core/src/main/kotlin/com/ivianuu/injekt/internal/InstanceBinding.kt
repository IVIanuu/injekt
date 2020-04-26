package com.ivianuu.injekt.internal

import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Parameters

internal class InstanceBinding<T>(private val instance: T) : LinkedBinding<T>() {
    override fun invoke(parameters: Parameters) = instance
}
