package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.UnlinkedBinding

internal object ComponentBinding : UnlinkedBinding<Component>() {
    override fun link(linker: Linker): LinkedBinding<Component> = InstanceBinding(linker.component)
}