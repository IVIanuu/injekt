package com.ivianuu.injekt.common

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.given
import io.kotest.matchers.shouldBe
import org.junit.Test

class WithFallbackTest {
    @Qualifier
    private annotation class Fallback

    @Test
    fun testPrefersActual() {
        @Given val actual = "actual"
        @Given val fallback: @Fallback String = "fallback"
        given<@WithFallback<@Fallback String> String>() shouldBe "actual"
    }

    @Test
    fun testFallback() {
        @Given val fallback: @Fallback String = "fallback"
        given<@WithFallback<@Fallback String> String>() shouldBe "fallback"
    }
}
