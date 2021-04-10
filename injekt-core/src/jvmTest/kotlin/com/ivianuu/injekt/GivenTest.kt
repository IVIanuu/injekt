package com.ivianuu.injekt

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class GivenTest {
    @Test
    fun testGiven() {
        @Given val value = "42"
        given<String>() shouldBe "42"
    }
    @Test
    fun testGivenOrNullReturnsExistingGiven() {
        @Given val value = "42"
        givenOrNull<String>() shouldBe "42"
    }
    @Test
    fun testGivenOrNullReturnsNullForUnexistingGiven() {
        givenOrNull<String>().shouldBeNull()
    }
}