package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.Component
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

object InjektPrototypeTest : InjectionTest {

    override val name: String = "Injekt prototype"

    private var component: Component? = null

    override fun setup() {
        /*val myComponent = Component {
            factory { Fib1() }
            factory { Fib2() }
            factory { Fib3(get(), get()) }
            factory { Fib4(get(), get()) }
            factory { Fib5(get(), get()) }
            factory { Fib6(get(), get()) }
            factory { Fib7(get(), get()) }
            factory { Fib8(get(), get()) }
        }*/
        component = ComponentImpl()
    }

    override fun inject() {
        component!!.get<Fib8>("com.ivianuu.injekt.comparison.fibonacci.Fib8")
    }

    override fun shutdown() {
        component = null
    }
}

private class ComponentImpl : Component {
    val fib1Provider = ComponentModule.fib1Provider
    val fib2Provider = ComponentModule.fib2Provider
    val fib3Provider = ComponentModule.fib3Provider(fib2Provider, fib1Provider)
    val fib4Provider = ComponentModule.fib4Provider(fib3Provider, fib2Provider)
    val fib5Provider = ComponentModule.fib5Provider(fib4Provider, fib3Provider)
    val fib6Provider = ComponentModule.fib6Provider(fib5Provider, fib4Provider)
    val fib7Provider = ComponentModule.fib7Provider(fib6Provider, fib5Provider)
    val fib8Provider = ComponentModule.fib8Provider(fib7Provider, fib6Provider)

    override fun <T> get(key: String): T {
        return when (key) {
            "com.ivianuu.injekt.comparison.fibonacci.Fib1" -> fib1Provider()
            "com.ivianuu.injekt.comparison.fibonacci.Fib2" -> fib2Provider()
            "com.ivianuu.injekt.comparison.fibonacci.Fib3" -> fib3Provider()
            "com.ivianuu.injekt.comparison.fibonacci.Fib4" -> fib4Provider()
            "com.ivianuu.injekt.comparison.fibonacci.Fib5" -> fib5Provider()
            "com.ivianuu.injekt.comparison.fibonacci.Fib6" -> fib6Provider()
            "com.ivianuu.injekt.comparison.fibonacci.Fib7" -> fib7Provider()
            "com.ivianuu.injekt.comparison.fibonacci.Fib8" -> fib8Provider()
            else -> error("Unexpected key $key")
        } as T
    }
}

private class ComponentModule {
    object fib1Provider : Provider<Fib1> {
        override fun invoke() = Fib1()
    }

    object fib2Provider : Provider<Fib2> {
        override fun invoke() = Fib2()
    }

    class fib3Provider(
        private val p1: Provider<Fib2>,
        private val p2: Provider<Fib1>
    ) : Provider<Fib3> {
        override fun invoke() = Fib3(p1(), p2())
    }

    class fib4Provider(
        private val p1: Provider<Fib3>,
        private val p2: Provider<Fib2>
    ) : Provider<Fib4> {
        override fun invoke() = Fib4(p1(), p2())
    }

    class fib5Provider(
        private val p1: Provider<Fib4>,
        private val p2: Provider<Fib3>
    ) : Provider<Fib5> {
        override fun invoke() = Fib5(p1(), p2())
    }

    class fib6Provider(
        private val p1: Provider<Fib5>,
        private val p2: Provider<Fib4>
    ) : Provider<Fib6> {
        override fun invoke() = Fib6(p1(), p2())
    }

    class fib7Provider(
        private val p1: Provider<Fib6>,
        private val p2: Provider<Fib5>
    ) : Provider<Fib7> {
        override fun invoke() = Fib7(p1(), p2())
    }

    class fib8Provider(
        private val p1: Provider<Fib7>,
        private val p2: Provider<Fib6>
    ) : Provider<Fib8> {
        override fun invoke() = Fib8(p1(), p2())
    }
}