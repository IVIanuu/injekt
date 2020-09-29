package com.ivianuu.injekt.samples.comparison.injekt

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.samples.comparison.fibonacci.Fib1
import com.ivianuu.injekt.samples.comparison.fibonacci.Fib2
import com.ivianuu.injekt.samples.comparison.fibonacci.Fib3
import com.ivianuu.injekt.samples.comparison.fibonacci.Fib4
import com.ivianuu.injekt.samples.comparison.fibonacci.Fib5
import com.ivianuu.injekt.samples.comparison.fibonacci.Fib6
import com.ivianuu.injekt.samples.comparison.fibonacci.Fib7
import com.ivianuu.injekt.samples.comparison.fibonacci.Fib8

@Component
interface InjektComponent {
    val fib8: Fib8

    interface Factory {
        fun create(): InjektComponent
    }
}

object InjektComponentFactoryImpl : InjektComponent.Factory {
    override fun create(): InjektComponent {
        return object : InjektComponent {
            override val fib8 get() = Fib8(fib7, fib6)
            private val fib7 get() = Fib7(fib6, fib5)
            private val fib6 get() = Fib6(fib5, fib4)
            private val fib5 get() = Fib5(fib4, fib3)
            private val fib4 get() = Fib4(fib3, fib2)
            private val fib3 get() = Fib3(fib2, fib1)
            private val fib2 get() = Fib2()
            private val fib1 get() = Fib1()
        }
    }
}
