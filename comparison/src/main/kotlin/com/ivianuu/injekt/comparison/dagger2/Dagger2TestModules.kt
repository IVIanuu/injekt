package com.ivianuu.injekt.comparison.dagger2

import com.ivianuu.injekt.comparison.base.InjectionTest

object Dagger2TestModules : InjectionTest {

    override val name = "Dagger 2 Modules"

    private var component: Dagger2ComponentModules? = null

    override fun setup() {
        component = DaggerDagger2ComponentModules.create()
    }

    override fun inject() {
        component!!.fib8
    }

    override fun shutdown() {
        component = null
    }
}