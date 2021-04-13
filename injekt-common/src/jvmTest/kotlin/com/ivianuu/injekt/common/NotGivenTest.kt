package com.ivianuu.injekt.common

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given
import com.ivianuu.injekt.givenOrNull
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.Test

class NotGivenTest {
    @Test
    fun availableIfNotGiven() {
        @Given val value: @NotGiven<String> Unit = Unit
        givenOrNull<Unit>().shouldNotBeNull()
    }
    @Test
    fun notAvailableIfGiven() {
        @Given val value: @NotGiven<String> Unit = Unit
        @Given val string = ""
        givenOrNull<Unit>().shouldBeNull()
    }
}