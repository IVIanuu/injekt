package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.createImpl

interface InjektTestComponent {
    val fib8: Fib8

    companion object {
        @Factory
        fun create(): InjektTestComponent = createImpl()
    }
}
