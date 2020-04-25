package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.AbstractProvider
import com.ivianuu.injekt.ApplicationScoped
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.comparison.base.InjectionTest
import com.ivianuu.injekt.comparison.fibonacci.Fib1
import com.ivianuu.injekt.comparison.fibonacci.Fib2
import com.ivianuu.injekt.comparison.fibonacci.Fib3
import com.ivianuu.injekt.comparison.fibonacci.Fib4
import com.ivianuu.injekt.comparison.fibonacci.Fib5
import com.ivianuu.injekt.comparison.fibonacci.Fib6
import com.ivianuu.injekt.comparison.fibonacci.Fib7
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.get
import com.ivianuu.injekt.keyOf

object InjektTest : InjectionTest {
    override val name = "Injekt"

    private var component: Component? = null

    override fun setup() {
        component = Component(ApplicationScoped::class, FibonacciModule)
    }

    override fun inject() {
        component!!.get<Fib8>()
    }

    override fun shutdown() {
        component = null
    }

}

private val FibonacciModule = Module {
    add(
        Binding(
            key = keyOf(),
            provider = object : Provider<Fib1> {
                override fun invoke(parameters: Parameters) = Fib1()
            }
        )
    )
    add(
        Binding(
            key = keyOf(),
            provider = object : Provider<Fib2> {
                override fun invoke(parameters: Parameters) = Fib2()
            }
        )
    )
    add(
        Binding(
            key = keyOf(),
            provider = object : AbstractProvider<Fib3>() {
                private lateinit var fibM1: Provider<Fib2>
                private lateinit var fibM2: Provider<Fib1>
                override fun link(linker: Linker) {
                    fibM1 = linker.get()
                    fibM2 = linker.get()
                }

                override fun invoke(parameters: Parameters) = Fib3(fibM1(), fibM2())
            }
        )
    )
    add(
        Binding(
            key = keyOf(),
            provider = object : AbstractProvider<Fib4>() {
                private lateinit var fibM1: Provider<Fib3>
                private lateinit var fibM2: Provider<Fib2>
                override fun link(linker: Linker) {
                    fibM1 = linker.get()
                    fibM2 = linker.get()
                }

                override fun invoke(parameters: Parameters) = Fib4(fibM1(), fibM2())
            }
        )
    )
    add(
        Binding(
            key = keyOf(),
            provider = object : AbstractProvider<Fib5>() {
                private lateinit var fibM1: Provider<Fib4>
                private lateinit var fibM2: Provider<Fib3>
                override fun link(linker: Linker) {
                    fibM1 = linker.get()
                    fibM2 = linker.get()
                }

                override fun invoke(parameters: Parameters) = Fib5(fibM1(), fibM2())
            }
        )
    )
    add(
        Binding(
            key = keyOf(),
            provider = object : AbstractProvider<Fib6>() {
                private lateinit var fibM1: Provider<Fib5>
                private lateinit var fibM2: Provider<Fib4>
                override fun link(linker: Linker) {
                    fibM1 = linker.get()
                    fibM2 = linker.get()
                }

                override fun invoke(parameters: Parameters) = Fib6(fibM1(), fibM2())
            }
        )
    )
    add(
        Binding(
            key = keyOf(),
            provider = object : AbstractProvider<Fib7>() {
                private lateinit var fibM1: Provider<Fib6>
                private lateinit var fibM2: Provider<Fib5>
                override fun link(linker: Linker) {
                    fibM1 = linker.get()
                    fibM2 = linker.get()
                }

                override fun invoke(parameters: Parameters) = Fib7(fibM1(), fibM2())
            }
        )
    )
    add(
        Binding(
            key = keyOf(),
            provider = object : AbstractProvider<Fib8>() {
                private lateinit var fibM1: Provider<Fib7>
                private lateinit var fibM2: Provider<Fib6>
                override fun link(linker: Linker) {
                    fibM1 = linker.get()
                    fibM2 = linker.get()
                }

                override fun invoke(parameters: Parameters) = Fib8(fibM1(), fibM2())
            }
        )
    )
}
