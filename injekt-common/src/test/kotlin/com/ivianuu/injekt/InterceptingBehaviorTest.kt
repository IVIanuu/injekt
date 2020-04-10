package com.ivianuu.injekt

import junit.framework.Assert
import junit.framework.Assert.assertEquals
import org.junit.Test

class InterceptingBehaviorTest {

    @Test
    fun testInterceptingBehavior() {
        var called = false
        val behavior = InterceptingBehavior {
            called = true
            it
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
    fun testInterceptingOrdering() {
        val appliedInterceptors = mutableListOf<InterceptingBehavior>()
        lateinit var behaviorA: InterceptingBehavior
        behaviorA =
            InterceptingBehavior { appliedInterceptors += behaviorA; it }
        lateinit var behaviorB: InterceptingBehavior
        behaviorB =
            InterceptingBehavior { appliedInterceptors += behaviorB; it }
        Component {
            bind(behavior = behaviorB + behaviorA) {}
        }

        assertEquals(listOf(behaviorB, behaviorA), appliedInterceptors)
    }

}