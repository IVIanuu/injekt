package com.ivianuu.injekt.comparison.magnet

import com.ivianuu.injekt.comparison.base.InjectionTest
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import magnet.Magnet
import magnet.Magnetizer
import magnet.Scope
import magnet.getSingle

object MagnetTest : InjectionTest {

    override val name = "Magnet"

    private var scope: Scope? = null

    override fun setup() {
        scope = Magnet.createRootScope()
    }

    override fun inject() {
        scope?.getSingle<Fib8>()
    }

    override fun shutdown() {
        scope = null
    }
}

@Magnetizer
interface TestMagnetizer