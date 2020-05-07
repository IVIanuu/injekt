package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.comparison.base.InjectionTest
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.createInstance

object InjektTest : InjectionTest {

    override val name = "Injekt"

    override fun setup() {
    }

    override fun inject() {
        createFib8()
    }

    override fun shutdown() {
    }

}

@Factory
fun createFib8(): Fib8 = createInstance()
