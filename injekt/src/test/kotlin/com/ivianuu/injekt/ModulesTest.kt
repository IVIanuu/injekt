package com.ivianuu.injekt

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class ModulesTest {

    @Test
    fun testOrdering() {
        val existingModules = Modules.modulesByScope.toMap()
        Modules.modulesByScope.clear()

        val initA = Module(invokeOnInit = true) { }
        val initB = Module(invokeOnInit = true) { }
        val nonInitA = Module(invokeOnInit = false) { }
        val nonInitB = Module(invokeOnInit = false) { }

        Injekt {
            modules(
                initA, nonInitA, nonInitB, initB
            )
        }

        assertEquals(listOf(initA, initB, nonInitA, nonInitB), Modules.get())
        Modules.modulesByScope.clear()
        Modules.modulesByScope += existingModules
    }

    @Test
    fun testGlobalModulesWillBeAppliedToEachOpenComponentBuilderWhenRegister() {
        var called = false
        Component {
            Injekt {
                module {
                    called = true
                }
            }
        }

        assertTrue(called)
    }

}
