package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import io.kotest.matchers.nulls.*
import org.junit.*

class NotGivenTest {
  @Test fun yes() {
    @Given fun value(_: NotProvided<String>) = Unit
    injectOrNull<Unit>().shouldNotBeNull()
  }

  @Test fun no() {
    @Given fun value(_: NotProvided<String>) = Unit
    @Given val string = ""
    injectOrNull<Unit>().shouldBeNull()
  }
}
