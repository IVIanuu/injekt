package com.ivianuu.injekt

/**
 * A behavior which allows to do something when ever a [Binding] gets added to a [ComponentBuilder]
 */
class SideEffectBehavior(val onBindingAdded: ComponentBuilder.(Binding<*>) -> Unit) :
    Behavior.Element

@Module
private val SideEffectBehaviorRunnerModule = Module(AnyScope, invokeOnInit = true) {
    onBindingAdded { binding ->
        binding.behavior.foldInBehavior(Unit) { _, element ->
            if (element is SideEffectBehavior) element.onBindingAdded(this, binding)
        }
    }
}
