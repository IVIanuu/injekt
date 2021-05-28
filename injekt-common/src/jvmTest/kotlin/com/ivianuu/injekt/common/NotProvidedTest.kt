package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import io.kotest.matchers.nulls.*
import org.junit.*

class NotProvidedTest {
  @Test fun yes() {
    @Provide fun value(ev: NotProvided<String>) = Unit
    injectOrNull<Unit>().shouldNotBeNull()
  }

  @Test fun no() {
    @Provide fun value(ev: NotProvided<String>) = Unit
    @Provide val string = ""
    injectOrNull<Unit>().shouldBeNull()
  }
}
