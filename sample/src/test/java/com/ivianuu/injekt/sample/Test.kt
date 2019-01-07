package com.ivianuu.injekt.sample

import com.ivianuu.injekt.component
import com.ivianuu.injekt.configureInjekt
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import com.ivianuu.injekt.printLogger
import com.ivianuu.injekt.test.check
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * @author Manuel Wrage (IVIanuu)
 */
class Test {

    @Test
    fun testModules() {
        configureInjekt { printLogger() }
        val component = component("test") {
            modules(
                module {
                    factory { mock(App::class.java) }
                    factory { mock(MainActivity::class.java) }
                    factory { mock(ParentFragment::class.java) }
                    factory { mock(ChildFragment::class.java) }
                },
                appModule, mainActivityModule, parentFragmentModule, childFragmentModule
            )
        }

        component.check()
    }

}