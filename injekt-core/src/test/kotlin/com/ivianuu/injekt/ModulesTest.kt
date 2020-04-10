package com.ivianuu.injekt

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class ModulesTest {

    @Test
    fun testOrdering() {
        val existingModules = Modules.modulesByScope.toMap()
        Modules.modulesByScope.clear()

        val initA = Module(AnyScope, invokeOnInit = true) { }
        val initB = Module(AnyScope, invokeOnInit = true) { }
        val nonInitA = Module(AnyScope, invokeOnInit = false) { }
        val nonInitB = Module(AnyScope, invokeOnInit = false) { }

        injekt {
            modules(
                initA, nonInitA, nonInitB, initB
            )
        }

        assertEquals(listOf(initA, initB, nonInitA, nonInitB), Modules.get(AnyScope))
        Modules.modulesByScope.clear()
        Modules.modulesByScope += existingModules
    }

    @Test
    fun testGlobalModulesWillBeAppliedToEachOpenComponentBuilderOnRegister() {
        var called = false
        Component {
            injekt {
                module(AnyScope) {
                    called = true
                }
            }
        }

        assertTrue(called)
    }

    @Test
    fun testAnyScopeWillBeAppliedToEveryComponent() {
        var called = false

        injekt {
            module(AnyScope) {
                called = true
            }
        }

        Component()

        assertTrue(called)
    }

    @Test
    fun testOnlyAppliedToSpecifiedScopes() {
        var called = false

        injekt {
            module(TestScope1) {
                called = true
            }
        }

        Component()
        assertFalse(called)

        Component { scopes(TestScope1) }
        assertTrue(called)

        assertTrue(called)
    }

}
