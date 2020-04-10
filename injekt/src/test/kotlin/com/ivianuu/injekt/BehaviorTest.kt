package com.ivianuu.injekt

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class BehaviorTest {

    @Test
    fun testContains() {
        val behavior = Factory + Single + Eager

        assertTrue(Factory in behavior)
        assertTrue(Single in behavior)
        assertTrue(Eager in behavior)
        assertTrue(Bound in behavior)
    }

    @Test
    fun testSideEffectBehavior() {
        var called = false
        val behavior = SideEffectBehavior("test") {
            called = true
        }
        Component {
            assertFalse(called)
            single { TestDep1() }
            assertFalse(called)
            single(behavior = behavior) { TestDep2(get()) }
            assertTrue(called)
        }
    }

    @Test
    fun testInterceptingBehavior() {
        var called = false
        val behavior = InterceptingBehavior("test") {
            called = true
            it
        }
        Component {
            assertFalse(called)
            single { TestDep1() }
            assertFalse(called)
            single(behavior = behavior) { TestDep2(get()) }
            assertTrue(called)
        }
    }

}
