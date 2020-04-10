package com.ivianuu.injekt

import com.ivianuu.injekt.internal.DeclarationName
import com.ivianuu.injekt.internal.declarationName

/**
 * A behavior which allows to do something when ever a [Binding] gets added to a [ComponentBuilder]
 */
class SideEffectBehavior(
    @DeclarationName val name: Any = declarationName(),
    val onBindingAdded: ComponentBuilder.(Binding<*>) -> Unit
) : Behavior.Element {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SideEffectBehavior

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name.toString()
}

@ModuleMarker
private val SideEffectBehaviorRunnerModule = Module(AnyScope, invokeOnInit = true) {
    onBindingAdded { binding ->
        binding.behavior.foldIn(Unit) { _, element ->
            if (element is SideEffectBehavior) element.onBindingAdded(this, binding)
        }
    }
}
