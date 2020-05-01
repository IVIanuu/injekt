package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.comparison.fibonacci.Fib1
import com.ivianuu.injekt.comparison.fibonacci.Fib2
import com.ivianuu.injekt.comparison.fibonacci.Fib3
import com.ivianuu.injekt.comparison.fibonacci.Fib4
import com.ivianuu.injekt.comparison.fibonacci.Fib5
import com.ivianuu.injekt.comparison.fibonacci.Fib6
import com.ivianuu.injekt.comparison.fibonacci.Fib7
import com.ivianuu.injekt.comparison.fibonacci.Fib8

interface InjektTestComponent {
    val fib8: Fib8

    companion object {
        //@Factory
        fun create(): InjektTestComponent =
            InjektTestComponentImpl() //createImplementation<InjektTestComponent>()
    }
}

private class InjektTestComponentImpl : InjektTestComponent {
    override val fib8: Fib8
        get() = get_7()

    private fun get_0() = Fib1()
    private fun get_1() = Fib2()
    private fun get_2() = Fib3(get_1(), get_0())
    private fun get_3() = Fib4(get_2(), get_1())
    private fun get_4() = Fib5(get_3(), get_2())
    private fun get_5() = Fib6(get_4(), get_3())
    private fun get_6() = Fib7(get_5(), get_4())
    private fun get_7() = Fib8(get_6(), get_5())
}
