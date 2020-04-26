package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.UnlinkedBinding

fun <T> Binding<T>.asScoped(): Binding<T> {
    return if (this is LinkedBinding && this !is LinkedScopedBinding) LinkedScopedBinding(this)
    else if (this is UnlinkedBinding && this !is UnlinkedScopedBinding) UnlinkedScopedBinding(this)
    else this
}

internal class UnlinkedScopedBinding<T>(
    private val wrapped: UnlinkedBinding<T>
) : UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> = LinkedScopedBinding(wrapped.link(linker));
}

internal class LinkedScopedBinding<T>(private var wrapped: LinkedBinding<T>?) : LinkedBinding<T>() {
    private var value: Any? = this

    override fun invoke(parameters: Parameters): T {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = wrapped!!(parameters)
                    this.value = value
                    wrapped = null
                }
            }
        }

        return value as T
    }
}
