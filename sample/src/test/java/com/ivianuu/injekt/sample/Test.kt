package com.ivianuu.injekt.sample

import com.ivianuu.injekt.*
import com.ivianuu.injekt.test.check
import org.junit.Test
import org.mockito.Mockito

/**
 * @author Manuel Wrage (IVIanuu)
 */
class Test {

    @Test
    fun testModules() {
        configureInjekt { printLogger() }
        val component = component {
            modules(
                module {
                    factory { Mockito.mock(App::class.java) }
                    factory { Mockito.mock(MainActivity::class.java) }
                    factory { Mockito.mock(ParentFragment::class.java) }
                    factory { Mockito.mock(ChildFragment::class.java) }
                },
                appModule, mainActivityModule, parentFragmentModule, childFragmentModule
            )
        }

        component.check()
    }

}