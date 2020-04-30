package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.createImplementation

interface InjektTestComponent {
    val fib8: Fib8
}

@Factory
fun createComponent() = createImplementation<InjektTestComponent>()
