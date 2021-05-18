package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import io.kotest.matchers.nulls.*
import org.junit.*

class NotProvidedTest {
  @Test fun yes() {
    @Provide fun value(_: NotProvided<String>) = Unit
    summonOrNull<Unit>().shouldNotBeNull()
  }

  @Test fun no() {
    @Provide fun value(_: NotProvided<String>) = Unit
    @Provide val string = ""
    summonOrNull<Unit>().shouldBeNull()
  }
}
