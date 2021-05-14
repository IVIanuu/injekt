package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import io.kotest.matchers.nulls.*
import org.junit.*

class NotGivenTest {
  @Test fun yes() {
    @Given fun value(@Given _: NotGiven<String>) = Unit
    givenOrNull<Unit>().shouldNotBeNull()
  }

  @Test fun no() {
    @Given fun value(@Given _: NotGiven<String>) = Unit
    @Given val string = ""
    givenOrNull<Unit>().shouldBeNull()
  }
}
