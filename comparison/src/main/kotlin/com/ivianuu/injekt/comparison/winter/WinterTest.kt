package com.ivianuu.injekt.comparison.winter

import com.ivianuu.injekt.comparison.base.InjectionTest
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import io.jentz.winter.Graph
import io.jentz.winter.Winter

object WinterTest : InjectionTest {

    override val name: String = "Winter"

    private var graph: Graph? = null

    override fun setup() {
        graph = Winter.createGraph { fib() }
    }

    override fun inject() {
        graph!!.instance<Fib8>()
    }

    override fun shutdown() {
        graph?.close()
    }

}