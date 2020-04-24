package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.comparison.base.InjectionTest
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.componentFactory

object InjektTest : InjectionTest {

    override val name = "Injekt"

    private var component: InjektComponent? = null

    override fun setup() {
        component = componentFactory<InjektComponent.Factory>()
            .create()
    }

    override fun inject() {
        component!!.fib8
    }

    override fun shutdown() {
        component = null
    }

}

@Component
interface InjektComponent {
    val fib8: Fib8
    @Component.Factory
    interface Factory {
        fun create(): InjektComponent
    }
}
