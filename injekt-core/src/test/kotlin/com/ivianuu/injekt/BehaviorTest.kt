package com.ivianuu.injekt

import junit.framework.Assert.assertTrue
import org.junit.Test

class BehaviorTest {

    @Test
    fun testContains() {
        val behaviorA = Behavior()
        val behaviorB = Behavior()
        val behaviorC = Behavior()
        val combined = behaviorA + behaviorB + behaviorC

        assertTrue(behaviorA in combined)
        assertTrue(behaviorB in combined)
        assertTrue(behaviorC in combined)
    }

}
