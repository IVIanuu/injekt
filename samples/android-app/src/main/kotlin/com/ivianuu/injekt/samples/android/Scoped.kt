package com.ivianuu.injekt.samples.android

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Decorator

@Decorator(MyComponent::class)
annotation class MyComponentScoped {
    companion object {
        fun <T> scope(factory: () -> T): () -> T {
            return factory
        }
    }
}

@MyComponentScoped
class Foo

@Component
abstract class MyComponent {
    abstract val foo: Foo
}
