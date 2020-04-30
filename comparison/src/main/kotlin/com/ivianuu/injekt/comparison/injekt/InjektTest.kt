package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.comparison.base.InjectionTest

object InjektTest : InjectionTest {
    override val name = "Injekt"

    private var component: InjektTestComponent? = null

    override fun setup() {
        component = createComponent()
    }

    override fun inject() {
        component!!.fib8
    }

    override fun shutdown() {
        component = null
    }

}
