package com.ivianuu.injekt

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
        val behavior = sideEffectBehavior("test") {
            called = true
        }
        Component {
            bind(behavior = behavior) { }
        }

        assertTrue(called)
    }

    @Test
    fun testInterceptingBehavior() {
        var called = false
        val behavior = interceptingBehavior("test") {
            called = true
            it
        }
        Component {
            bind(behavior = behavior) { }
        }

        assertTrue(called)
    }

}