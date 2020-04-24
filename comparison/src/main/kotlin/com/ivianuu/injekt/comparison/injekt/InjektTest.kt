package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.ApplicationScope
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.comparison.base.InjectionTest
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.get

object InjektTest : InjectionTest {
    override val name = "Injekt"

    private var component: Component? = null

    override fun setup() {
        component = Component { scopes(ApplicationScope) }
    }

    override fun inject() {
        component!!.get<Fib8>()
    }

    override fun shutdown() {

    }

}
