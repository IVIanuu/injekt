/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt

import io.kotest.matchers.*
import org.junit.*

class InjectTest {
  @Test fun testInject() {
    @Provide val value = "42"
    inject<String>() shouldBe "42"
  }
}
