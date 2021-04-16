package com.ivianuu.injekt

import io.kotest.matchers.*
import io.kotest.matchers.nulls.*
import org.junit.*

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