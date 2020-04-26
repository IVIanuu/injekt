package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.ApplicationScoped
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.comparison.base.InjectionTest
import com.ivianuu.injekt.comparison.fibonacci.Fib1
import com.ivianuu.injekt.comparison.fibonacci.Fib2
import com.ivianuu.injekt.comparison.fibonacci.Fib3
import com.ivianuu.injekt.comparison.fibonacci.Fib4
import com.ivianuu.injekt.comparison.fibonacci.Fib5
import com.ivianuu.injekt.comparison.fibonacci.Fib6
import com.ivianuu.injekt.comparison.fibonacci.Fib7
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get

object InjektTest : InjectionTest {
    override val name = "Injekt"

    private var component: Component? = null

    override fun setup() {
        component = Component<ApplicationScoped>(
            Module {
                factory { Fib1() }
                factory { Fib2() }
                factory { Fib3(get(), get()) }
                factory { Fib4(get(), get()) }
                factory { Fib5(get(), get()) }
                factory { Fib6(get(), get()) }
                factory { Fib7(get(), get()) }
                factory { Fib8(get(), get()) }
            }
        )
    }

    override fun inject() {
        component!!.get<Fib8>()
    }

    override fun shutdown() {
        component = null
    }

}
