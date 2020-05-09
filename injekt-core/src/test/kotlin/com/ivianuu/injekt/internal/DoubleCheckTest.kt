package com.ivianuu.injekt.internal

import junit.framework.Assert.assertEquals
import org.junit.Test

class DoubleCheckTest {

    @Test
    fun testDoubleCheck() {
        var callCount = 0
        val doubleCheck = DoubleCheck {
            callCount++
        }

        assertEquals(0, callCount)
        doubleCheck()
        assertEquals(1, callCount)
        doubleCheck()
        assertEquals(1, callCount)
    }

}