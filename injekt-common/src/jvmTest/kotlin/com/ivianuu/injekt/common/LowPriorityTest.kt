package com.ivianuu.injekt.common

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.Test

class LowPriorityTest {
    @Test
    fun testPrefersOtherOverLowPriority() {
        @Given val lowPriority: @LowPriority Boolean = false
        @Given val normal: Boolean = true
        given<Boolean>().shouldBeTrue()
    }
}
