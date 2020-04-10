package com.ivianuu.injekt

import com.ivianuu.injekt.internal.DeclarationName
import com.ivianuu.injekt.internal.declarationName

/**
 * A behavior which allows to modify [Binding]s before they get added to a [ComponentBuilder]
 *
 * @see Bound
 * @see Factory
 * @see Single
 *
 */
class InterceptingBehavior(
    @DeclarationName val name: Any = declarationName(),
    val intercept: ComponentBuilder.(Binding<Any?>) -> Binding<Any?>?
) : Behavior.Element {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InterceptingBehavior

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name.toString()
}

@ModuleMarker
private val InterceptingBehaviorRunnerModule = Module(AnyScope, invokeOnInit = true) {
    bindingInterceptor { binding ->
        binding.behavior.foldIn(binding) { currentBinding: Binding<Any?>?, element ->
            if (currentBinding != null && element is InterceptingBehavior) element.intercept(
                this,
                currentBinding
            )
            else currentBinding
        }
    }
}