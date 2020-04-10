package com.ivianuu.injekt

import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import org.junit.Test
import kotlin.concurrent.thread

class ThreadLocalTest {

    @Test
    fun testThreadLocalBehavior() {
        val component = Component {
            threadLocal { TestDep1() }
        }

        lateinit var valueB: TestDep1
        val t = thread {
            val valueA = component.get<TestDep1>()
            valueB = component.get()
            assertSame(valueA, valueB)
        }

        t.join()

        val valueC = component.get<TestDep1>()
        assertNotSame(valueB, valueC)
    }

}