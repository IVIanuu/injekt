package com.ivianuu.injekt

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class SideEffectBehaviorTest {

    @Test
    fun testSideEffectBehavior() {
        var called = false
        val behavior = SideEffectBehavior {
            called = true
        }
        Component {
            assertFalse(called)
            com.ivianuu.injekt.single { TestDep1() }
            assertFalse(called)
            single(behavior = behavior) { TestDep2(get()) }
            assertTrue(called)
        }
    }

    @Test
    fun testSideEffectOrdering() {
        val appliedSideEffects = mutableListOf<SideEffectBehavior>()
        lateinit var behaviorA: SideEffectBehavior
        behaviorA =
            SideEffectBehavior { appliedSideEffects += behaviorA }
        lateinit var behaviorB: SideEffectBehavior
        behaviorB =
            SideEffectBehavior { appliedSideEffects += behaviorB }
        Component {
            bind(behavior = behaviorB + behaviorA) {}
        }

        assertEquals(listOf(behaviorB, behaviorA), appliedSideEffects)
    }

}