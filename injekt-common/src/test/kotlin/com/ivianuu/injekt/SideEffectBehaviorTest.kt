package com.ivianuu.injekt

import junit.framework.Assert
import junit.framework.Assert.assertEquals
import org.junit.Test

class SideEffectBehaviorTest {

    @Test
    fun testSideEffectBehavior() {
        var called = false
        val behavior = SideEffectBehavior("test") {
            called = true
        }
        Component {
            Assert.assertFalse(called)
            single { TestDep1() }
            Assert.assertFalse(called)
            single(behavior = behavior) { TestDep2(get()) }
            Assert.assertTrue(called)
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