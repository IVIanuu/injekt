package com.ivianuu.injekt.internal

import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Parameters

internal object NullBinding : LinkedBinding<Nothing?>() {
    override fun invoke(parameters: Parameters) = null
}
