package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.comparison.fibonacci.Fib400
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.createImplementation

interface InjektTestComponent {
    val fib8: Fib8
    val fib400: Fib400

    companion object {
        @Factory
        fun create(): InjektTestComponent = createImplementation()
    }
}
