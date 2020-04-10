package com.ivianuu.injekt

import junit.framework.Assert.assertEquals
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
    fun testSideEffectOrdering() {
        val appliedSideEffects = mutableListOf<SideEffectBehavior>()
        lateinit var behaviorA: SideEffectBehavior
        behaviorA = SideEffectBehavior { appliedSideEffects += behaviorA }
        lateinit var behaviorB: SideEffectBehavior
        behaviorB = SideEffectBehavior { appliedSideEffects += behaviorB }
        Component {
            bind(behavior = behaviorB + behaviorA) {}
        }

        assertEquals(listOf(behaviorB, behaviorA), appliedSideEffects)
    }

    @Test
    fun testInterceptingBehavior() {
        var called = false
        val behavior = InterceptingBehavior {
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

    @Test
    fun testInterceptingOrdering() {
        val appliedInterceptors = mutableListOf<InterceptingBehavior>()
        lateinit var behaviorA: InterceptingBehavior
        behaviorA = InterceptingBehavior { appliedInterceptors += behaviorA; it }
        lateinit var behaviorB: InterceptingBehavior
        behaviorB = InterceptingBehavior { appliedInterceptors += behaviorB; it }
        Component {
            bind(behavior = behaviorB + behaviorA) {}
        }

        assertEquals(listOf(behaviorB, behaviorA), appliedInterceptors)
    }
}
