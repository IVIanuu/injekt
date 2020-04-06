package com.ivianuu.injekt

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class ModulesTest {

    @Test
    fun testOrdering() {
        val existingModules = Modules.modulesByScope.toMap()
        Modules.modulesByScope.clear()

        val initA = object : Module.Impl {
            override val invokeOnInit: Boolean
                get() = true

            override fun apply(builder: ComponentBuilder) {
            }
        }
        val initB = object : Module.Impl {
            override val invokeOnInit: Boolean
                get() = true

            override fun apply(builder: ComponentBuilder) {
            }
        }
        val nonInitA = object : Module.Impl {
            override val invokeOnInit: Boolean
                get() = false

            override fun apply(builder: ComponentBuilder) {
            }
        }
        val nonInitB = object : Module.Impl {
            override val invokeOnInit: Boolean
                get() = false

            override fun apply(builder: ComponentBuilder) {
            }
        }

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
