package com.ivianuu.injekt

/**
 * A behavior which allows to modify [Binding]s before they get added to a [ComponentBuilder]
 *
 * @see Bound
 * @see Factory
 * @see Single
 *
 */
class InterceptingBehavior(val intercept: ComponentBuilder.(Binding<Any?>) -> Binding<Any?>?) :
    Behavior.Element

@Module
private val InterceptingBehaviorRunnerModule = Module(AnyScope, invokeOnInit = true) {
    bindingInterceptor { binding ->
        binding.behavior.foldInBehavior(binding) { currentBinding: Binding<Any?>?, element ->
            if (currentBinding != null && element is InterceptingBehavior) element.intercept(
                this,
                currentBinding
            )
            else currentBinding
        }
    }
}